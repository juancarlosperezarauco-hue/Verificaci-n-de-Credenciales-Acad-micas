import os
import unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from app.main import app


class SecurityTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)
        self.token = "a-disposable-service-token-for-tests-only"

    def test_health_is_public_but_document_analysis_requires_authentication(self):
        with patch.dict(os.environ, {"AI_SERVICE_TOKEN": self.token}):
            self.assertEqual(200, self.client.get("/health").status_code)
            response = self.client.post("/identity-analysis", json={
                "images": ["invalid"], "expected_name": "Ana Perez", "expected_document": "12345"})
            self.assertEqual(401, response.status_code)

    def test_missing_service_configuration_fails_closed(self):
        with patch.dict(os.environ, {"AI_SERVICE_TOKEN": ""}):
            response = self.client.post("/summaries", json={"text": "Contenido académico ficticio para resumir"})
            self.assertEqual(503, response.status_code)

    def test_validation_errors_do_not_echo_private_input(self):
        with patch.dict(os.environ, {"AI_SERVICE_TOKEN": self.token}):
            response = self.client.post("/identity-analysis", headers={"X-Service-Token": self.token},
                json={"images": [], "expected_name": "PRIVATE-NAME", "expected_document": "1"})
            self.assertEqual(422, response.status_code)
            self.assertNotIn("PRIVATE-NAME", response.text)

    def test_oversized_body_is_rejected(self):
        response = self.client.post("/identity-analysis", content=b"x" * (9 * 1024 * 1024 + 1))
        self.assertEqual(413, response.status_code)


if __name__ == "__main__":
    unittest.main()
