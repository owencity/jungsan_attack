import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from receipt_ocr.model_files import verify_models


class ModelFilesTests(unittest.TestCase):
    def test_modified_or_missing_model_is_rejected_without_downloading(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            manifest = directory / "manifest.json"
            model = directory / "example"
            model.mkdir()
            path = model / "inference.json"
            payload = b"verified model"
            manifest.write_text(json.dumps({"example": {"files": {
                "inference.json": hashlib.sha256(payload).hexdigest()}}}), encoding="utf-8")
            with patch("receipt_ocr.model_files.MANIFEST", manifest):
                with self.assertRaisesRegex(ValueError, "OCR_MODEL_FILES_INVALID"):
                    verify_models(directory)
                path.write_bytes(payload)
                self.assertEqual({"example": model}, verify_models(directory))
                path.write_bytes(b"changed")
                with self.assertRaisesRegex(ValueError, "OCR_MODEL_FILES_INVALID"):
                    verify_models(directory)


class WatchdogTests(unittest.TestCase):
    def run_child(self, body):
        script = "from receipt_ocr.watchdog import ProcessWatchdog\nimport time\n" + body
        return subprocess.run([sys.executable, "-c", script], capture_output=True, text=True, timeout=10)

    def test_stuck_process_is_terminated_without_waiting_for_its_thread(self):
        result = self.run_child(
            "watchdog = ProcessWatchdog()\n"
            "with watchdog.deadline(0.3, 'request'):\n"
            "    time.sleep(30)\n")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("RECEIPT_DEADLINE_EXCEEDED stage=request", result.stderr)

    def test_completed_request_cancels_deadline_and_next_request_can_run(self):
        result = self.run_child(
            "watchdog = ProcessWatchdog()\n"
            "try:\n"
            "    with watchdog.deadline(0.3, 'request'):\n"
            "        pass\n"
            "    time.sleep(0.7)\n"
            "    with watchdog.deadline(2, 'request'):\n"
            "        time.sleep(0.1)\n"
            "finally:\n"
            "    watchdog.close()\n")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn("RECEIPT_DEADLINE_EXCEEDED", result.stderr)


if __name__ == "__main__":
    unittest.main()
