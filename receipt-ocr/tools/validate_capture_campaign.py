"""고정 정답으로 공개 사진을 대조한다. 사진·OCR 원문은 출력하지 않는다."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from receipt_ocr.dictionary import load_dictionary
from receipt_ocr.extract import extract
from receipt_ocr.images import decode_image
from receipt_ocr.ocr import PaddleEngine


class TimedEngine(PaddleEngine):
    def _recognize_once(self, image):
        started = time.perf_counter()
        try:
            return super()._recognize_once(image)
        finally:
            self.base_seconds = time.perf_counter() - started

    def _recognize_numeric(self, image):
        started = time.perf_counter()
        try:
            return super()._recognize_numeric(image)
        finally:
            self.numeric_seconds += time.perf_counter() - started
            self.numeric_calls += 1


def compare_result(case, result):
    checks = {"confirmationRequired": result["requiresConfirmation"] is True}
    if "total" in case:
        checks["total"] = result["total"] == case["total"]
    if "drinks" in case:
        checks["drinkCount"] = len(result["drinks"]) == len(case["drinks"])
        # 순서는 영수증 위→아래다. 부분 정답 필드만 검사하며 9필드 정확도로 부르지 않는다.
        for index, expected in enumerate(case["drinks"]):
            actual = result["drinks"][index] if index < len(result["drinks"]) else {}
            for field, value in expected.items():
                checks[f"drink{index}.{field}"] = field in actual and actual[field] == value
    if "warning" in case:
        checks["warning"] = case["warning"] in result["warnings"]
    if case.get("noInventedPrices"):
        checks["noInventedPrices"] = result["total"] is None and all(
            drink["unitPrice"] is None and drink["lineAmount"] is None for drink in result["drinks"])
    return checks


def inspect_case(case, samples, dictionary, engine):
    started = time.perf_counter()
    engine.numeric_seconds = 0
    engine.numeric_calls = 0
    record = {"sample": case["id"], "cohort": case["cohort"], "family": case["family"]}
    try:
        payload = (samples / case["image"]).read_bytes()
        record["sha256"] = hashlib.sha256(payload).hexdigest()
        if record["sha256"] != case["sha256"]:
            raise ValueError("sample hash differs from labelled original")
        with decode_image(payload) as image:
            result = extract(engine.recognize(image), dictionary).to_dict()
        record.update({"result": result, "checks": compare_result(case, result),
                       "baseSeconds": round(engine.base_seconds, 3),
                       "numericSeconds": round(engine.numeric_seconds, 3), "numericCalls": engine.numeric_calls})
    except Exception as error:
        # 실패도 관측에 남기되 예외 메시지의 경로·OCR 원문은 기록하지 않는다.
        record["errorType"] = type(error).__name__
    record["seconds"] = round(time.perf_counter() - started, 3)
    return record


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("samples", type=Path, help="사용자가 준비한 로컬 원본 폴더")
    parser.add_argument("--manifest", type=Path, default=ROOT / "examples/manifest.capture-campaign.example.json")
    parser.add_argument("--output", type=Path, help="선택: 로컬 보고서 저장 위치. 공개 사진도 Git에 넣지 않는다")
    arguments = parser.parse_args()
    if arguments.output:
        arguments.output.parent.mkdir(parents=True, exist_ok=True)
    manifest_bytes = arguments.manifest.read_bytes()
    cases = json.loads(manifest_bytes.decode("utf-8-sig"))
    dictionary = load_dictionary(ROOT / "data/drinks.md")
    started = time.perf_counter()
    engine = TimedEngine()
    report = {"manifestSha256": hashlib.sha256(manifest_bytes).hexdigest(),
              "modelStartupSeconds": round(time.perf_counter() - started, 3), "observations": []}
    for case in cases:
        record = inspect_case(case, arguments.samples, dictionary, engine)
        report["observations"].append(record)
        print(json.dumps(record, ensure_ascii=True), flush=True)
        if arguments.output:
            arguments.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    observations = report["observations"]
    completed = [record for record in observations if "checks" in record]
    seconds = [record["seconds"] for record in completed]
    report["summary"] = {"samples": len(observations), "families": len({case["family"] for case in cases}),
                         "executionFailures": len(observations) - len(completed),
                         "allAnnotatedChecksMatch": sum(all(record["checks"].values()) for record in completed),
                         "medianSeconds": statistics.median(seconds) if seconds else None,
                         "maxSeconds": max(seconds) if seconds else None}
    if arguments.output:
        arguments.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=True), flush=True)
    # 도전 사례의 알려진 실패도 성공 종료로 숨기지 않는다. 정답은 그대로 유지한다.
    return int(report["summary"]["allAnnotatedChecksMatch"] != report["summary"]["samples"])


if __name__ == "__main__":
    raise SystemExit(main())
