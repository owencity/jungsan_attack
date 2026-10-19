"""GitHub Actions 전용 OCR 계약 대역. 추론·사진 보관·외부 접속은 하지 않는다."""
import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

assert os.environ.get("GITHUB_ACTIONS") == "true", "CI 전용이다."
KEY = os.environ["RECEIPT_OCR_SERVICE_KEY"]
FIXTURE = (Path(__file__).resolve().parents[1] / "server/src/test/resources/receipt/response-v2.json").read_bytes()
calls = 0

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def reply(self, status, body):
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/calls":
            self.reply(200, json.dumps({"calls": calls}).encode())
        else:
            self.reply(404, b'{}')

    def do_POST(self):
        global calls
        if self.path != "/api/v1/receipts/extract":
            self.reply(404, b'{}')
            return
        if self.headers.get("X-Receipt-Service-Key") != KEY:
            self.reply(401, b'{"detail":{"code":"SERVICE_AUTH_REQUIRED"}}')
            return
        length = int(self.headers.get("Content-Length", "0"))
        if not 0 < length <= 8 * 1024 * 1024 or self.headers.get("Content-Type") not in ("image/jpeg", "image/png"):
            self.reply(400, b'{"detail":{"code":"IMAGE_INVALID"}}')
            return
        self.rfile.read(length)
        calls += 1
        self.reply(200, FIXTURE)

HTTPServer(("127.0.0.1", 18082), Handler).serve_forever()
