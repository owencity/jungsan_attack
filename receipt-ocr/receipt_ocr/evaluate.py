"""로컬 영수증 정답과 비교한다. 사진·OCR 원문·개별 인식 결과는 보고서에 기록하지 않는다."""
import argparse
from collections import Counter
import json
from pathlib import Path
import time

from .dictionary import load_dictionary
from .extract import extract
from .images import decode_image, MAX_BYTES
from .ocr import PaddleEngine


def drink_key(drink):
    return tuple(drink[field] for field in ("name", "category", "unit", "servingType", "unitPrice",
                                           "quantity", "lineAmount", "volumeMl", "bottleCount"))


def evaluate(manifest: Path, dictionary_path: Path, engine):
    records = json.loads(manifest.read_text(encoding="utf-8-sig"))
    if not isinstance(records, list) or not records:
        raise ValueError("평가 목록에는 최소 한 장이 필요합니다")
    dictionary = load_dictionary(dictionary_path)
    total_matches = drink_matches = failures = review_count = 0
    latencies = []
    for record in records:
        # null 정답으로 인식 실패를 정답으로 세지 않는다.
        if not isinstance(record["total"], int) or isinstance(record["total"], bool) or record["total"] < 0:
            raise ValueError("정답 총액은 0 이상 정수여야 합니다")
        expected_drinks = Counter(drink_key(drink) for drink in record["drinks"])
        image_path = manifest.parent / record["image"]
        started = time.perf_counter()
        try:
            with image_path.open("rb") as image_file:
                payload = image_file.read(MAX_BYTES + 1)
            with decode_image(payload) as image:
                result = extract(engine.recognize(image), dictionary).to_dict()
        except Exception:
            failures += 1
            continue
        latencies.append(time.perf_counter() - started)
        total_matches += result["total"] == record["total"]
        drink_matches += Counter(drink_key(drink) for drink in result["drinks"]) == expected_drinks
        review_count += bool(result["warnings"])
    count = len(records)
    return {"samples": count, "failedSamples": failures,
            "totalExactRate": total_matches / count, "allDrinkFieldsExactRate": drink_matches / count,
            "warningSampleCount": review_count,
            "meanSecondsExcludingStartup": sum(latencies) / len(latencies) if latencies else None}


def main():
    parser = argparse.ArgumentParser(description="실제 영수증 정답 기준 총액·주류 필드 정확도 측정")
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--dictionary", type=Path, default=Path(__file__).resolve().parents[1] / "data/drinks.md")
    arguments = parser.parse_args()
    print(json.dumps(evaluate(arguments.manifest, arguments.dictionary, PaddleEngine()), indent=2))


if __name__ == "__main__":
    main()
