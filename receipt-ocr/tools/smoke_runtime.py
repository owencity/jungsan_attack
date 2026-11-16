"""실제 컨테이너 추론 점검. 가짜 영수증만 만들며 응답 본문·키를 출력하지 않는다."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from io import BytesIO
import json
import os
from pathlib import Path
from threading import Barrier
import time
import urllib.error
import urllib.request

from PIL import Image, ImageDraw, ImageFont


def request(payload: bytes, *, key: str | None = None):
    headers = {"Content-Type": "image/png", "X-Receipt-Service-Key": key or os.environ["RECEIPT_SERVICE_KEY"]}
    req = urllib.request.Request("http://127.0.0.1:8091/api/v1/receipts/extract", data=payload, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=45) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def receipt(font_path: str) -> bytes:
    # 이름·상호·전화·카드 정보가 없는 합성 문서다. 실제 사진 정확도 평가를 대체하지 않는다.
    font = ImageFont.truetype(font_path, 34)
    with Image.new("RGB", (1100, 850), "white") as image:
        draw = ImageDraw.Draw(image)
        draw.text((60, 50), "테스트 영수증", font=font, fill="black")
        rows = [
            ("품목", "단가", "수량", "금액"),
            ("참이슬", "5,000", "3", "15,000"),
            ("안주", "35,000", "1", "35,000"),
        ]
        for row_index, row in enumerate(rows):
            for x, value in zip((60, 400, 650, 830), row):
                draw.text((x, 170 + row_index * 100), value, font=font, fill="black")
        draw.text((60, 620), "합계", font=font, fill="black")
        draw.text((830, 620), "50,000", font=font, fill="black")
        buffer = BytesIO()
        image.save(buffer, format="PNG")
        return buffer.getvalue()


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def run(font_path: str):
    started = time.monotonic()
    payload = receipt(font_path)
    status, result = request(payload)
    check(status == 200, "synthetic receipt request failed")
    check(result["schemaVersion"] == 2 and result["requiresConfirmation"] is True, "confirmation contract changed")
    check(result["total"] == 50000, "synthetic receipt total mismatch")
    check(any(item["quantity"] == 3 and item["lineAmount"] == 15000 for item in result["drinks"]),
          "synthetic receipt alcohol item mismatch")
    inference_seconds = time.monotonic() - started

    with Image.new("RGB", (300, 300), "white") as image:
        buffer = BytesIO()
        image.save(buffer, format="PNG")
    status, result = request(buffer.getvalue())
    check(status == 200 and result["total"] is None and result["drinks"] == [], "blank image became trusted data")
    check(result["requiresConfirmation"] is True, "blank image confirmation missing")
    status, result = request(b"not an image")
    check(status == 400 and result["detail"]["code"] == "IMAGE_INVALID", "damaged image not rejected")
    status, _ = request(payload, key="invalid-test-key")
    check(status == 401, "service authentication not enforced")

    barrier = Barrier(3)
    def concurrent_request():
        barrier.wait(timeout=5)
        return request(payload)
    with ThreadPoolExecutor(max_workers=3) as pool:
        responses = list(pool.map(lambda _: concurrent_request(), range(3)))
    check(sum(status == 200 for status, _ in responses) == 1, "more than one concurrent inference admitted")
    check(sum(status == 503 and body.get("detail", {}).get("code") == "OCR_BUSY" for status, body in responses) == 2,
          "concurrent requests did not return OCR_BUSY")
    status, result = request(payload)
    check(status == 200 and result["total"] == 50000, "capacity did not recover after busy or invalid requests")
    print(json.dumps({"synthetic_smoke": "passed", "first_inference_seconds": round(inference_seconds, 3)}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--font", required=True)
    args = parser.parse_args()
    if not Path(args.font).is_file():
        raise SystemExit("Korean test font not found")
    run(args.font)
