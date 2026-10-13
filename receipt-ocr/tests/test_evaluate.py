from io import BytesIO
import json
from pathlib import Path
import tempfile
import unittest
from PIL import Image
from receipt_ocr.evaluate import evaluate
from test_extract import tokens


class EvaluationTests(unittest.TestCase):
    def test_failed_images_remain_in_accuracy_denominator(self):
        class Engine:
            def recognize(self, image):
                return tokens("품목 단가 수량 금액", "참이슬 5,000 3 15,000", "합계 50,000")
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            buffer = BytesIO()
            with Image.new("RGB", (32, 32)) as image:
                image.save(buffer, format="PNG")
            (root / "good.png").write_bytes(buffer.getvalue())
            expected = {"total": 50000, "drinks": [{"name": "참이슬", "unit": "BOTTLE", "unitPrice": 5000, "quantity": 3}]}
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps([{**expected, "image": "good.png"}, {**expected, "image": "missing.png"}]), encoding="utf-8")
            dictionary = Path(__file__).resolve().parents[1] / "data/drinks.md"
            result = evaluate(manifest, dictionary, Engine())
            self.assertEqual(2, result["samples"])
            self.assertEqual(1, result["failedSamples"])
            self.assertEqual(.5, result["totalExactRate"])
            self.assertEqual(.5, result["allDrinkFieldsExactRate"])
            self.assertNotIn("image", result)
