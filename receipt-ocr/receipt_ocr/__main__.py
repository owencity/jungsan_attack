import argparse
import json
from pathlib import Path
from .dictionary import load_dictionary
from .extract import extract
from .model import Token


def main():
    parser = argparse.ArgumentParser(description="영수증 확인용 총액·주류 후보 추출")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--image", type=Path)
    source.add_argument("--tokens", type=Path, help="로컬 검증용 OCR 토큰 JSON")
    parser.add_argument("--dictionary", type=Path, default=Path(__file__).resolve().parents[1] / "data/drinks.md")
    arguments = parser.parse_args()
    dictionary = load_dictionary(arguments.dictionary)
    if arguments.tokens:
        records = json.loads(arguments.tokens.read_text(encoding="utf-8-sig"))
        tokens = [Token(record["text"], record["confidence"], tuple(record["box"])) for record in records]
    else:
        from .images import decode_image
        from .ocr import PaddleEngine
        from .images import MAX_BYTES
        with arguments.image.open("rb") as image_file:
            payload = image_file.read(MAX_BYTES + 1)
        with decode_image(payload) as image:
            tokens = PaddleEngine().recognize(image)
    print(json.dumps(extract(tokens, dictionary).to_dict(), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
