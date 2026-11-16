from dataclasses import dataclass
from pathlib import Path
import re
import unicodedata


CATEGORIES = {"소주": "SOJU", "맥주": "BEER", "막걸리": "MAKGEOLLI",
              "청주약주": "CHEONGJU_YAKJU", "와인": "WINE", "위스키증류주": "WHISKY_SPIRIT",
              "혼합주": "COCKTAIL_MIXED", "기타주류": "OTHER_ALCOHOL",
              "비주류": "NON_ALCOHOL", "비음료": "NON_BEVERAGE", "미상": "UNKNOWN"}
UNITS = {"병": "BOTTLE", "캔": "CAN", "잔": "GLASS", "피처": "PITCHER", "팩": "PACK", "미상": "UNKNOWN"}
HEADER = ["영수증 표기", "주류 종류", "표시 이름", "단위"]
EXTENDED_HEADER = HEADER + ["제공 방식"]
SERVING_TYPES = {"미상": "UNKNOWN", "생맥": "DRAFT"}
UNIT_MARKERS = {"병": r"병|\bbottles?\b", "캔": r"캔|\bcans?\b",
                "잔": r"잔|샷|\bglass(?:es)?\b", "피처": r"피처|피쳐|\bpitcher\b",
                "팩": r"팩|(?:병|캔)입|\bpack\b"}


def normalize(text: str) -> str:
    # 띄어쓰기·대소문자만 허용한다. 유사 문자열 추측으로 논알코올을 술로 바꾸지 않는다.
    return re.sub(r"\s+", "", unicodedata.normalize("NFKC", text)).casefold()


@dataclass(frozen=True)
class Entry:
    category: str
    name: str
    unit: str
    serving_type: str = "UNKNOWN"


def load_dictionary(path: Path) -> dict[str, Entry]:
    entries = {}
    header = None
    for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip().startswith("|"):
            if line.strip():
                header = None
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if cells in (HEADER, EXTENDED_HEADER):
            header = cells
            continue
        if header is None:
            continue
        if all(re.fullmatch(r":?-+:?", cell) for cell in cells):
            continue
        if len(cells) != len(header) or not all(cells):
            raise ValueError(f"사전 {line_number}행: 표의 칸을 모두 채워야 합니다")
        alias, category, name, unit = cells[:4]
        serving_type = cells[4] if len(cells) == 5 else "미상"
        if category not in CATEGORIES or unit not in UNITS:
            raise ValueError(f"사전 {line_number}행: 종류 또는 단위가 잘못됐습니다")
        if serving_type not in SERVING_TYPES:
            raise ValueError(f"사전 {line_number}행: 제공 방식이 잘못됐습니다")
        if unit != "미상" and not re.search(UNIT_MARKERS[unit], unicodedata.normalize("NFKC", alias), re.I):
            raise ValueError(f"사전 {line_number}행: 브랜드의 용기를 추측하지 말고 단위를 미상으로 적으세요")
        if serving_type == "생맥" and normalize(alias) not in {"생맥", "생맥주", "드래프트비어", "draftbeer"}:
            raise ValueError(f"사전 {line_number}행: 생맥 제공 방식은 일반 생맥 메뉴 표기에만 지정하세요")
        if len(alias) > 100 or len(name) > 100:
            raise ValueError(f"사전 {line_number}행: 표기·이름은 100자 이하여야 합니다")
        key = normalize(alias)
        entry = Entry(CATEGORIES[category], name, UNITS[unit], SERVING_TYPES[serving_type])
        if key in entries and entries[key] != entry:
            raise ValueError(f"사전 {line_number}행: 같은 표기에 다른 분류가 있습니다")
        entries[key] = entry
    if not entries:
        raise ValueError("주류 사전 표가 비어 있습니다")
    return entries
