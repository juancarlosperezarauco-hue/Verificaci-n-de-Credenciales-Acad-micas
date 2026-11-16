import os
import secrets
from fastapi import Header, HTTPException


def require_service_token(x_service_token: str | None = Header(default=None)) -> None:
    expected = os.getenv("AI_SERVICE_TOKEN", "")
    if len(expected) < 32:
        raise HTTPException(503, "El servicio de asistencia no está configurado.")
    if not x_service_token or not secrets.compare_digest(expected, x_service_token):
        raise HTTPException(401, "Acceso no autorizado.")


class BodyLimit:
    """Bound request bodies even when the client omits Content-Length."""
    def __init__(self, app, limit=9 * 1024 * 1024):
        self.app, self.limit = app, limit

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http" or scope["method"] not in {"POST", "PUT", "PATCH"}:
            return await self.app(scope, receive, send)
        body = bytearray()
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                return
            body.extend(message.get("body", b""))
            if len(body) > self.limit:
                await send({"type": "http.response.start", "status": 413,
                            "headers": [(b"content-type", b"application/json")]})
                await send({"type": "http.response.body", "body": b'{"detail":"Solicitud demasiado grande."}'})
                return
            if not message.get("more_body"):
                break
        delivered = False

        async def replay():
            nonlocal delivered
            if not delivered:
                delivered = True
                return {"type": "http.request", "body": bytes(body), "more_body": False}
            return await receive()
        await self.app(scope, replay, send)
