import os

from fastapi import FastAPI, HTTPException
from google import genai
from pydantic import BaseModel, Field

app = FastAPI(title="Credential AI Service", version="0.1.0")


class SummaryRequest(BaseModel):
    text: str = Field(min_length=20, max_length=20_000)


class SummaryResponse(BaseModel):
    summary: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/summaries", response_model=SummaryResponse)
def summarize(request: SummaryRequest) -> SummaryResponse:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=503, detail="GEMINI_API_KEY no está configurada")
    client = genai.Client(api_key=api_key)
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=f"Resume en español este contenido académico sin inventar datos:\n\n{request.text}",
    )
    return SummaryResponse(summary=response.text or "")
