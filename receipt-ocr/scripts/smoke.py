"""합성 영수증 한 장으로 실제 모델과 업로드 경로를 검증한다. 실제 사진 정확도 평가가 아니다."""
import json
import os
from pathlib import Path
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
os.environ.setdefault("PADDLE_PDX_CACHE_HOME", str(ROOT / ".models/paddlex"))
os.environ.setdefault("HF_HOME", str(ROOT / ".models/huggingface"))

from fastapi.testclient import TestClient
from PIL import Image, ImageDraw, ImageFont
from receipt_ocr.api import create_app
from receipt_ocr.ocr import PaddleEngine


def main():
    font_path = Path(os.environ.get("RECEIPT_SMOKE_FONT", "C:/Windows/Fonts/malgun.ttf"))
    font = ImageFont.truetype(str(font_path), 36)
    image = Image.new("RGB", (1000, 440), "white")
    draw = ImageDraw.Draw(image)
    rows = [("품목", "단가", "수량", "금액"), ("참이슬", "5,000", "3", "15,000"),
            ("테라", "6,000", "2", "12,000"), ("안주", "23,000", "1", "23,000")]
    for index, cells in enumerate(rows):
        for x, cell in zip((40, 350, 590, 750), cells):
            draw.text((x, 35 + index * 70), cell, font=font, fill="black")
    draw.text((40, 340), "합계", font=font, fill="black")
    draw.text((750, 340), "50,000", font=font, fill="black")
    from io import BytesIO
    payload = BytesIO()
    image.save(payload, format="PNG")
    image.close()
    started = time.perf_counter()
    engine = PaddleEngine()
    initialization = time.perf_counter() - started
    key = "synthetic-smoke-key-at-least-32-chars"
    with TestClient(create_app(dictionary_path=ROOT / "data/drinks.md", service_key=key, engine=engine)) as client:
        started = time.perf_counter()
        response = client.post("/api/v1/receipts/extract", content=payload.getvalue(),
                               headers={"Content-Type": "image/png", "X-Receipt-Service-Key": key})
        inference = time.perf_counter() - started
        print(json.dumps({"status": response.status_code, "initializationSeconds": round(initialization, 3),
                          "inferenceSeconds": round(inference, 3), "result": response.json()}, ensure_ascii=False, indent=2))
        assert response.status_code == 200
        result = response.json()
        assert result["total"] == 50000
        assert [(drink["name"], drink["unitPrice"], drink["quantity"]) for drink in result["drinks"]] == [("참이슬", 5000, 3), ("테라", 6000, 2)]
        assert result["requiresConfirmation"]


if __name__ == "__main__":
    main()
