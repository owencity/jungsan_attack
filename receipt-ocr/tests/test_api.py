from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
from pathlib import Path
from threading import Event
import unittest

from fastapi.testclient import TestClient
from PIL import Image

from receipt_ocr.api import create_app
from receipt_ocr.images import decode_image, InvalidImage, MAX_BYTES
from test_extract import tokens

DICTIONARY = Path(__file__).resolve().parents[1] / "data/drinks.md"
KEY = "test-only-service-key-with-32-characters"
HEADERS = {"Content-Type": "image/png", "X-Receipt-Service-Key": KEY}


def png():
    buffer = BytesIO()
    with Image.new("RGB", (32, 32), "white") as image:
        image.save(buffer, format="PNG")
    return buffer.getvalue()


class FakeEngine:
    def recognize(self, image):
        return tokens("품목 단가 수량 금액", "참이슬 5,000 3 15,000", "합계 50,000")


class ApiTests(unittest.TestCase):
    def app(self, engine=None):
        return create_app(dictionary_path=DICTIONARY, service_key=KEY, engine=engine or FakeEngine())

    def test_success(self):
        with TestClient(self.app()) as client:
            response = client.post("/api/v1/receipts/extract", content=png(), headers=HEADERS)
            self.assertEqual(200, response.status_code)
            self.assertEqual(50000, response.json()["total"])
            self.assertEqual(3, response.json()["drinks"][0]["quantity"])
            self.assertTrue(response.json()["requiresConfirmation"])

    def test_missing_or_wrong_service_key_rejected(self):
        with TestClient(self.app()) as client:
            for key in ("", "wrong"):
                response = client.post("/api/v1/receipts/extract", content=png(), headers={**HEADERS, "X-Receipt-Service-Key": key})
                self.assertEqual(401, response.status_code)

    def test_invalid_content_type_and_image(self):
        with TestClient(self.app()) as client:
            self.assertEqual(415, client.post("/api/v1/receipts/extract", content=b"bad", headers={**HEADERS, "Content-Type": "application/pdf"}).status_code)
            response = client.post("/api/v1/receipts/extract", content=b"bad", headers=HEADERS)
            self.assertEqual(400, response.status_code)
            self.assertEqual("IMAGE_INVALID", response.json()["detail"]["code"])

    def test_upload_limit_even_without_content_length(self):
        with TestClient(self.app()) as client:
            chunks = (b"x" * (1024 * 1024) for unused in range(9))
            response = client.post("/api/v1/receipts/extract", content=chunks, headers=HEADERS)
            self.assertEqual(413, response.status_code)

    def test_ocr_failure_is_sanitized_and_capacity_recovers(self):
        class FailingEngine:
            def recognize(self, image):
                raise RuntimeError("private-card-12345678")
        with TestClient(self.app(FailingEngine())) as client:
            for unused in range(2):
                response = client.post("/api/v1/receipts/extract", content=png(), headers=HEADERS)
                self.assertEqual("OCR_UNAVAILABLE", response.json()["detail"]["code"])
                self.assertNotIn("12345678", response.text)

    def test_concurrent_request_rejected_without_unbounded_queue(self):
        entered, release = Event(), Event()
        class BlockingEngine:
            def recognize(self, image):
                entered.set()
                if not release.wait(5):
                    raise RuntimeError("test worker timed out")
                return FakeEngine().recognize(image)
        with TestClient(self.app(BlockingEngine())) as client, ThreadPoolExecutor(max_workers=1) as pool:
            first = pool.submit(client.post, "/api/v1/receipts/extract", content=png(), headers=HEADERS)
            try:
                self.assertTrue(entered.wait(3))
                second = client.post("/api/v1/receipts/extract", content=png(), headers=HEADERS)
                self.assertEqual(503, second.status_code)
                self.assertEqual("OCR_BUSY", second.json()["detail"]["code"])
            finally:
                release.set()
            self.assertEqual(200, first.result(timeout=3).status_code)

    def test_missing_configuration_fails_fast(self):
        with self.assertRaises(ValueError):
            create_app(dictionary_path=DICTIONARY, service_key="", engine=FakeEngine())


class ImageTests(unittest.TestCase):
    def test_file_size_limit(self):
        with self.assertRaises(InvalidImage):
            decode_image(b"x" * (MAX_BYTES + 1))

    def test_pixel_limit(self):
        buffer = BytesIO()
        with Image.new("1", (4001, 4000)) as image:
            image.save(buffer, format="PNG")
        with self.assertRaisesRegex(InvalidImage, "IMAGE_DIMENSIONS_TOO_LARGE"):
            decode_image(buffer.getvalue())

    def test_metadata_removed_and_dimensions_limited(self):
        buffer = BytesIO()
        with Image.new("RGB", (2500, 32)) as image:
            image.save(buffer, format="PNG")
        with decode_image(buffer.getvalue()) as image:
            self.assertEqual(2400, image.width)
            self.assertEqual({}, image.info)

    def test_unsupported_actual_format_is_rejected(self):
        buffer = BytesIO()
        with Image.new("RGB", (32, 32)) as image:
            image.save(buffer, format="GIF")
        with self.assertRaisesRegex(InvalidImage, "IMAGE_FORMAT_UNSUPPORTED"):
            decode_image(buffer.getvalue())


if __name__ == "__main__":
    unittest.main()
