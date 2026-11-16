import base64
import io
import os
import unittest
from unittest.mock import patch
from PIL import Image
from fastapi import HTTPException
from app.ocr import AnalysisRequest, analyze, compare, read_image


class OcrTests(unittest.TestCase):
    def encoded(self):
        buffer = io.BytesIO()
        Image.new("RGB", (100, 80), "white").save(buffer, format="PNG")
        return base64.b64encode(buffer.getvalue()).decode()

    def test_explicit_signals_never_validate_identity(self):
        signals = compare("OTRA PERSONA 99999", 0.5, "Ana Pérez", "12345")
        self.assertEqual({"LOW_OCR_CONFIDENCE", "DOCUMENT_NOT_FOUND", "NAME_NOT_FOUND"},
                         {s.code for s in signals})

    def test_diacritics_and_document_punctuation_do_not_create_false_alerts(self):
        self.assertEqual([], compare("ANA PEREZ CI 12345LP", 0.98, "Ana Pérez", "12345-LP"))

    def test_image_is_parsed_and_ocr_result_is_explainable(self):
        data = {"text": ["Ana", "Pérez", "12345"], "conf": ["90", "80", "100"]}
        with patch("app.ocr.pytesseract.image_to_data", return_value=data):
            result = analyze(AnalysisRequest(images=[self.encoded()],
                expected_name="Ana Pérez", expected_document="12345"))
        self.assertTrue(result.human_review_required)
        self.assertEqual(0.9, result.confidence)
        self.assertEqual([], result.signals)

    def test_rejects_invalid_base64_and_fake_images(self):
        for value in ["!!!!", base64.b64encode(b"not an image").decode()]:
            with self.assertRaises(HTTPException) as error:
                read_image(value)
            self.assertEqual(400, error.exception.status_code)

    def test_rejects_oversized_images_before_decoding(self):
        with self.assertRaises(HTTPException) as error:
            read_image("A" * 4_194_305)
        self.assertEqual(413, error.exception.status_code)

    def test_timeout_is_reported_without_leaking_document_data(self):
        with patch("app.ocr.pytesseract.image_to_data", side_effect=RuntimeError("private document text")):
            with self.assertRaises(HTTPException) as error:
                read_image(self.encoded())
        self.assertEqual(503, error.exception.status_code)
        self.assertNotIn("private", error.exception.detail)

    @unittest.skipUnless(os.environ.get("OCR_NATIVE_TEST") == "true", "Native OCR runs in its Docker image")
    def test_native_tesseract_reads_a_fictional_document(self):
        from PIL import ImageDraw, ImageFont
        image = Image.new("RGB", (1500, 400), "white")
        draw = ImageDraw.Draw(image)
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 58)
        draw.text((40, 70), "ANA PEREZ\nCI 12345678", font=font, fill="black", spacing=30)
        data = io.BytesIO()
        image.save(data, format="PNG")
        result = analyze(AnalysisRequest(images=[base64.b64encode(data.getvalue()).decode()],
            expected_name="Ana Perez", expected_document="12345678"))
        self.assertIn("12345678", result.text)
        self.assertTrue(result.human_review_required)
        self.assertNotIn("DOCUMENT_NOT_FOUND", [s.code for s in result.signals])


if __name__ == "__main__":
    unittest.main()
