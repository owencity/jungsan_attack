"""상품명에 명시된 용량·단위만 읽는다. 가격·용량으로 판매 수량을 복원하지 않는다."""
from dataclasses import dataclass
from fractions import Fraction
import re
import unicodedata

from .dictionary import Entry, normalize


MAX_QUANTITY = 1000
MAX_VOLUME_ML = 100_000
INTEGER = r"(?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)"
# 잘못된 소수·부호·쉼표도 통째로 읽어 거절한다. 숫자 일부를 정수로 구제하지 않는다.
NUMERIC_TOKEN = r"[+\-−]?[0-9]+(?:[.,][0-9]+)*"
LEFT_BOUNDARY = r"(?<![0-9.,+\-−])"
RIGHT_BOUNDARY = r"(?![A-Za-z가-힣0-9])"
UNIT_NAMES = {"병": "BOTTLE", "bottle": "BOTTLE", "bottles": "BOTTLE",
              "캔": "CAN", "can": "CAN", "cans": "CAN",
              "잔": "GLASS", "glass": "GLASS", "glasses": "GLASS",
              "피처": "PITCHER", "피쳐": "PITCHER", "pitcher": "PITCHER"}
UNIT_PATTERN = r"(?:bottles?|cans?|glass(?:es)?|pitcher|병|캔|잔|피처|피쳐)"
COUNT = re.compile(rf"{LEFT_BOUNDARY}({NUMERIC_TOKEN})\s*({UNIT_PATTERN}){RIGHT_BOUNDARY}", re.I)
LABELLED_COUNT = re.compile(rf"(?:수량\s*[:：]?\s*|[x×]\s*)({NUMERIC_TOKEN})(?![0-9.,A-Za-z가-힣])", re.I)
VOLUME = re.compile(rf"{LEFT_BOUNDARY}({NUMERIC_TOKEN})\s*(ml|mℓ|㎖|cc|㏄|밀리|L|ℓ|리터){RIGHT_BOUNDARY}", re.I)
PACK = re.compile(rf"{LEFT_BOUNDARY}({NUMERIC_TOKEN})\s*(?:캔|병)\s*입{RIGHT_BOUNDARY}")
CONTAINER = re.compile(r"\s*(병|캔|잔|피처|피쳐|BOTTLE|CAN|GLASS|PITCHER)\s*$", re.I)
QUALIFIED_DRAFT = re.compile(r"^(생맥주|생맥|드래프트비어|draft\s*beer)\s*\(([^()]+)\)$", re.I)


def read_integer(text: str, maximum: int, *, allow_zero: bool = False) -> int | None:
    if not re.fullmatch(INTEGER, text):
        return None
    # 비정상적으로 긴 OCR 숫자가 Python 정수 변환 제한/불필요한 자원을 건드리지 않게 한다.
    digits = text.replace(",", "")
    if len(digits) > len(str(maximum)):
        return None
    value = int(digits)
    return value if (0 if allow_zero else 1) <= value <= maximum else None


def read_volume(text: str, unit: str) -> int | None:
    if len(text) > 16 or not re.fullmatch(rf"{INTEGER}(?:\.[0-9]+)?", text):
        return None
    volume = Fraction(text.replace(",", ""))
    if unit.casefold() in {"l", "ℓ", "리터"}:
        volume *= 1000
    if volume.denominator != 1 or not 0 < volume <= MAX_VOLUME_ML:
        return None
    return int(volume)


@dataclass
class Product:
    entry: Entry
    unit: str
    quantity: int | None
    volume_ml: int | None
    reasons: list[str]


def _agree(values: list, reason: str, reasons: list[str]):
    if not values:
        return None
    if None in values or len(set(values)) != 1:
        reasons.append(reason)
        return None
    return values[0]


def _qualified_draft_entry(text: str, dictionary: dict[str, Entry]) -> Entry | None:
    # 실제 메뉴의 '생맥주(브랜드 용량)'만 조합한다. 임의 괄호나 비알코올 접미어를 버리지 않는다.
    match = QUALIFIED_DRAFT.fullmatch(text.strip())
    if not match:
        return None
    generic = dictionary.get(normalize(match[1]))
    brand = dictionary.get(normalize(match[2]))
    if (generic is None or generic.category != "BEER" or generic.serving_type != "DRAFT"
            or brand is None or brand.category != "BEER" or brand.unit != "UNKNOWN"
            or brand.serving_type != "UNKNOWN"):
        return None
    return Entry("BEER", brand.name, "UNKNOWN", "DRAFT")


def match_product(text: str, dictionary: dict[str, Entry]) -> Product | None:
    text = unicodedata.normalize("NFKC", text).strip()
    # 테라 제로·소주잔처럼 단위나 숫자가 제품명인 정확한 전체 표기를 먼저 보호한다.
    direct_entry = dictionary.get(normalize(text))
    if direct_entry and direct_entry.category in {"NON_ALCOHOL", "NON_BEVERAGE"}:
        return Product(direct_entry, direct_entry.unit, None, None, [])
    reasons = []
    quantities, volumes, units = [], [], []

    def remove_pack(match):
        units.append("PACK")
        reasons.append("COMBO_OR_PROMOTION")
        return " "

    def remove_volume(match):
        volumes.append(read_volume(match[1], match[2]))
        return " "

    def remove_count(match):
        quantities.append(read_integer(match[1], MAX_QUANTITY))
        units.append(UNIT_NAMES[match[2].casefold()])
        return " "

    def remove_labelled_count(match):
        quantities.append(read_integer(match[1], MAX_QUANTITY))
        return " "

    remainder = PACK.sub(remove_pack, text)
    remainder = VOLUME.sub(remove_volume, remainder)
    remainder = COUNT.sub(remove_count, remainder)
    remainder = LABELLED_COUNT.sub(remove_labelled_count, remainder)
    remainder = remainder.strip(" /")
    entry = dictionary.get(normalize(remainder))
    if entry is None:
        container_match = CONTAINER.search(remainder)
        if container_match:
            units.append(UNIT_NAMES[container_match[1].casefold()])
            remainder = remainder[:container_match.start()]
        entry = dictionary.get(normalize(remainder))
    if entry is None:
        entry = _qualified_draft_entry(remainder, dictionary)
    if entry is None:
        entry = direct_entry
    if entry is None:
        return None
    quantity = _agree(quantities, "QUANTITY_INVALID_OR_CONFLICT", reasons)
    volume = _agree(volumes, "VOLUME_INVALID_OR_CONFLICT", reasons)
    if entry.unit != "UNKNOWN" and "PACK" not in units:
        units.append(entry.unit)
    unit = _agree(units, "UNIT_CONFLICT", reasons) or "UNKNOWN"
    if entry.serving_type == "DRAFT" and unit == "BOTTLE":
        reasons.append("UNIT_CONFLICT")
        unit = "UNKNOWN"
    return Product(entry, unit, quantity, volume, list(dict.fromkeys(reasons)))
