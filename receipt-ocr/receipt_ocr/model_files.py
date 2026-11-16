"""운영은 평가에 쓴 가중치만 읽으며, 누락된 모델을 요청 중 내려받지 않는다."""
import hashlib
import json
from pathlib import Path
import urllib.request


MANIFEST = Path(__file__).resolve().parents[1] / "models.lock.json"


def verify_models(directory: Path) -> dict[str, Path]:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    result = {}
    for name, entry in manifest.items():
        model_directory = directory / name
        for filename, expected in entry["files"].items():
            path = model_directory / filename
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
                raise ValueError("OCR_MODEL_FILES_INVALID")
        result[name] = model_directory
    return result


def download_models(directory: Path) -> None:
    # 이 함수는 이미지 빌드에서만 호출한다. 운영 API는 verify_models만 호출한다.
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    for name, entry in manifest.items():
        model_directory = directory / name
        model_directory.mkdir(parents=True, exist_ok=True)
        for filename, expected in entry["files"].items():
            url = f"https://huggingface.co/PaddlePaddle/{name}/resolve/{entry['revision']}/{filename}"
            with urllib.request.urlopen(url, timeout=120) as response:
                payload = response.read(32 * 1024 * 1024 + 1)
            if len(payload) > 32 * 1024 * 1024 or hashlib.sha256(payload).hexdigest() != expected:
                raise ValueError("OCR_MODEL_DOWNLOAD_INVALID")
            (model_directory / filename).write_bytes(payload)
    verify_models(directory)


if __name__ == "__main__":
    import sys
    download_models(Path(sys.argv[1]))
