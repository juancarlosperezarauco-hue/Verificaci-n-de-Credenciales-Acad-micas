"""OCR assistance: signals for a human reviewer, never an identity verdict."""
import base64
import binascii
import io
import re
import unicodedata
import warnings
from threading import BoundedSemaphore

from fastapi import HTTPException
from PIL import Image
import pytesseract
from pydantic import BaseModel, Field

Image.MAX_IMAGE_PIXELS = 8_000_000
_worker = BoundedSemaphore(1)


class AnalysisRequest(BaseModel):
    images: list[str] = Field(min_length=1, max_length=2)
    expected_name: str = Field(min_length=2, max_length=180)
    expected_document: str = Field(min_length=2, max_length=80)


class Signal(BaseModel):
    code: str
    explanation: str


class AnalysisResponse(BaseModel):
    text: str
    confidence: float
    signals: list[Signal]
    human_review_required: bool = True
    engine: str = "tesseract"


def normalized(value: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFKD", value).upper()
                   if c.isalnum())


def compare(text: str, confidence: float, name: str, document: str) -> list[Signal]:
    signals = []
    if confidence < 0.65:
        signals.append(Signal(code="LOW_OCR_CONFIDENCE", explanation=
            "La lectura automática tiene baja confianza. Revisa la imagen original."))
    folded = normalized(text)
    if normalized(document) not in folded:
        signals.append(Signal(code="DOCUMENT_NOT_FOUND", explanation=
            "No se encontró el número esperado en el texto leído. Puede ser un error del OCR."))
    words = [normalized(w) for w in name.split() if len(normalized(w)) > 2]
    if not words or any(word not in folded for word in words):
        signals.append(Signal(code="NAME_NOT_FOUND", explanation=
            "No se encontró el nombre completo esperado. Comprueba sus variantes y la imagen."))
    return signals


def read_image(encoded: str) -> tuple[str, list[float]]:
    if len(encoded) > 4_194_304:
        raise HTTPException(413, "Cada imagen debe pesar hasta 3 MiB.")
    try:
        content = base64.b64decode(encoded, validate=True)
        if not content or len(content) > 3 * 1024 * 1024:
            raise HTTPException(413, "Cada imagen debe pesar hasta 3 MiB.")
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(io.BytesIO(content), formats=["JPEG", "PNG"]) as source:
                if source.width * source.height > Image.MAX_IMAGE_PIXELS:
                    raise ValueError("Image too large")
                source.load()
                image = source.convert("RGB")
        try:
            data = pytesseract.image_to_data(image, lang="spa+eng",
                config="--psm 6", timeout=8, output_type=pytesseract.Output.DICT)
        finally:
            image.close()
        tokens, confidences = [], []
        for text, confidence in zip(data["text"], data["conf"]):
            if text.strip():
                tokens.append(text.strip())
                number = float(confidence)
                if number >= 0:
                    confidences.append(min(number, 100) / 100)
        return " ".join(tokens)[:12000], confidences
    except HTTPException:
        raise
    except (binascii.Error, ValueError, OSError, Image.DecompressionBombError,
            Image.DecompressionBombWarning) as error:
        raise HTTPException(400, "Adjunta imágenes JPEG o PNG válidas de hasta 8 megapíxeles.") from error
    except (RuntimeError, pytesseract.TesseractNotFoundError) as error:
        raise HTTPException(503, "El OCR no está disponible o excedió su tiempo máximo.") from error


def analyze(request: AnalysisRequest) -> AnalysisResponse:
    if not _worker.acquire(blocking=False):
        raise HTTPException(429, "El OCR está procesando otro documento. Inténtalo nuevamente.")
    try:
        texts, scores = [], []
        for image in request.images:
            text, confidence = read_image(image)
            texts.append(text)
            scores.extend(confidence)
        text = "\n".join(texts)
        confidence = round(sum(scores) / len(scores), 4) if scores else 0.0
        return AnalysisResponse(text=text, confidence=confidence,
            signals=compare(text, confidence, request.expected_name, request.expected_document))
    finally:
        _worker.release()
