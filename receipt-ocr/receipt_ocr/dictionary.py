from dataclasses import dataclass
from pathlib import Path
import re
import unicodedata


CATEGORIES = {"소주": "SOJU", "맥주": "BEER", "막걸리": "MAKGEOLLI",
              "기타주류": "OTHER", "비주류": "NON_ALCOHOL"}
UNITS = {"병": "BOTTLE", "캔": "CAN", "잔": "GLASS"}
HEADER = ["영수증 표기", "주류 종류", "표시 이름", "단위"]


def normalize(text: str) -> str:
    # 띄어쓰기·대소문자만 허용한다. 유사 문자열 추측으로 논알코올을 술로 바꾸지 않는다.
    return re.sub(r"\s+", "", unicodedata.normalize("NFKC", text)).casefold()


@dataclass(frozen=True)
class Entry:
    category: str
    name: str
    unit: str


def load_dictionary(path: Path) -> dict[str, Entry]:
    entries = {}
    in_table = False
    for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip().startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if cells == HEADER:
            in_table = True
            continue
        if not in_table:
            continue
        if all(re.fullmatch(r":?-+:?", cell) for cell in cells):
            continue
        if len(cells) != 4 or not all(cells):
            raise ValueError(f"사전 {line_number}행: 네 칸을 모두 채워야 합니다")
        alias, category, name, unit = cells
        if category not in CATEGORIES or unit not in UNITS:
            raise ValueError(f"사전 {line_number}행: 종류 또는 단위가 잘못됐습니다")
        if len(alias) > 100 or len(name) > 100:
            raise ValueError(f"사전 {line_number}행: 표기·이름은 100자 이하여야 합니다")
        key = normalize(alias)
        entry = Entry(CATEGORIES[category], name, UNITS[unit])
        if key in entries and entries[key] != entry:
            raise ValueError(f"사전 {line_number}행: 같은 표기에 다른 분류가 있습니다")
        entries[key] = entry
    if not entries:
        raise ValueError("주류 사전 표가 비어 있습니다")
    return entries
