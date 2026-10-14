"""명시된 표의 비어 있는 작은 숫자 칸만 재인식한다. 누락 값을 계산하거나 기본값으로 채우지 않는다."""
from dataclasses import dataclass, replace
import math
import re

from PIL import Image, ImageOps, ImageStat

from .extract import HEADER_LABELS, HEADER_TEXT, MIN_CONFIDENCE, SUPPORTED_COLUMNS, group_rows
from .model import Token
from .product import INTEGER, MAX_QUANTITY, read_integer


MAX_RECOVERY_CELLS = 8
PATCH_VIEWS = ((4, 16), (4, 32))
MIN_RECOVERY_CONFIDENCE = .90
LABEL_PART = re.compile(r"단\s*가|수\s*량|할\s*인|금\s*액")
FOOTER = re.compile(r"^(?:합계|총액|총금액|총매출|결제|공급가액|과세|부가|면세|할인금액|카드|승인|가맹|거스름|받은)")


@dataclass(frozen=True)
class NumericRegion:
    label: str
    box: tuple[int, int, int, int]
    confidence: float
    amount_token: Token


def _header_cells(row: list[Token]) -> dict[str, Token] | None:
    normalized = re.sub(r"\s+", "", "".join(token.text for token in row))
    labels = HEADER_LABELS.findall(normalized)
    if (not HEADER_TEXT.fullmatch(normalized) or len(labels) != len(set(labels))
            or set(labels) not in SUPPORTED_COLUMNS or min(token.confidence for token in row) < MIN_CONFIDENCE):
        return None
    cells = {}
    for token in row:
        text = token.text.strip()
        matches = list(LABEL_PART.finditer(text))
        # 한 상자에 붙은 두 숫자 라벨까지만 위치를 나눈다. 상품명까지 합친 넓은 헤더는 추측하지 않는다.
        if not matches or len(matches) > 2 or LABEL_PART.sub("", text).strip():
            continue
        left, top, right, bottom = token.box
        for match in matches:
            label = re.sub(r"\s+", "", match[0])
            cells[label] = Token(label, token.confidence,
                                 (left + (right-left) * match.start()/len(text), top,
                                  left + (right-left) * match.end()/len(text), bottom))
    return cells if set(cells) == set(labels) else None


def _column_bounds(cell: Token, cells: dict[str, Token]) -> tuple[float, float]:
    left, top, right, bottom = cell.box
    padding = .18 * (bottom-top)
    center = (left+right)/2
    before = [(other.box[0]+other.box[2])/2 for other in cells.values()
              if (other.box[0]+other.box[2])/2 < center]
    after = [(other.box[0]+other.box[2])/2 for other in cells.values()
             if (other.box[0]+other.box[2])/2 > center]
    return (max(left-padding, (max(before)+center)/2 if before else left-padding),
            min(right+padding, (min(after)+center)/2 if after else right+padding))


def _regions(image: Image.Image, tokens: list[Token]):
    cells = None
    for row in group_rows(tokens):
        text = re.sub(r"\s+", "", "".join(token.text for token in row))
        if HEADER_TEXT.fullmatch(text) and HEADER_LABELS.search(text):
            cells = _header_cells(row)
            continue
        if FOOTER.search(text):
            cells = None
        if not cells:
            continue
        money_left, money_right = _column_bounds(cells["금액"], cells)
        money = [token for token in row if money_left <= (token.box[0]+token.box[2])/2 <= money_right
                 and re.fullmatch(rf"{INTEGER}원?", re.sub(r"\s+", "", token.text))
                 and token.confidence >= MIN_CONFIDENCE]
        if len(money) != 1:
            continue
        anchor = money[0]
        height = anchor.box[3]-anchor.box[1]
        first_column = min(cell.box[0] for cell in cells.values())
        if not any(token.box[2] < first_column and re.search(r"[A-Za-z가-힣]", token.text) for token in row):
            continue
        if anchor.box[1] <= max(cell.box[3] for cell in cells.values()) or height > 80:
            continue
        for label in ("수량", "할인"):
            if label not in cells:
                continue
            left, right = _column_bounds(cells[label], cells)
            # 이미 읽은 숫자·부호·인접 상품명이 있으면 덮어쓰거나 일부만 다시 읽지 않는다.
            if any(token.box[0] < right and token.box[2] > left for token in row):
                continue
            box = (max(0, math.floor(left)), max(0, math.floor(anchor.box[1]-.1*height)),
                   min(image.width, math.ceil(right)), min(image.height, math.ceil(anchor.box[3]+.1*height)))
            if 4 <= box[2]-box[0] <= 160 and 8 <= box[3]-box[1] <= 96:
                yield NumericRegion(label, box, min(cell.confidence for cell in cells.values()), anchor)


def _prepare_cell(image: Image.Image, region: NumericRegion) -> Image.Image | None:
    with image.crop(region.box) as patch, patch.convert("L") as gray:
        background = int(ImageStat.Stat(gray).median[0])
        with gray.point(lambda value: 255 if value < background-40 else 0) as ink:
            bounds = ink.getbbox()
            ink_count = ink.histogram()[255]
        if bounds is None or ink_count < 8 or ink_count > gray.width * gray.height * .35:
            return None
        left, top, right, bottom = bounds
        if left < 2 or right > gray.width-2 or top < 2 or bottom > gray.height-2 or bottom-top < 6:
            return None
        # 좌우는 자르지 않아 음수 부호·소수점을 보존한다. 금액 행 높이로 표의 가로 구분선도 피한다.
        margin = max(2, math.ceil((bottom-top)*.2))
        with gray.crop((0, max(0, top-margin), gray.width, min(gray.height, bottom+margin))) as trimmed:
            with ImageOps.autocontrast(trimmed) as contrast:
                result = contrast.convert("RGB")
                result.info.clear()
                return result


def recover_numeric_regions(image: Image.Image, tokens: list[Token], recognize_patch) -> list[Token]:
    result = list(tokens)
    for attempted, region in enumerate(_regions(image, tokens)):
        if attempted >= MAX_RECOVERY_CELLS:
            break
        prepared = _prepare_cell(image, region)
        if prepared is None:
            continue
        readings = []
        conflicting = False
        with prepared:
            for scale, border in PATCH_VIEWS:
                with prepared.resize((prepared.width*scale, prepared.height*scale), Image.Resampling.LANCZOS) as enlarged:
                    with ImageOps.expand(enlarged, border=border, fill="white") as padded:
                        candidates = recognize_patch(padded)
                if not candidates or max(token.confidence for token in candidates) < MIN_RECOVERY_CONFIDENCE:
                    continue
                if len(candidates) != 1:
                    conflicting = True
                    break
                token = candidates[0]
                text = token.text.strip()
                if read_integer(text, MAX_QUANTITY if region.label == "수량" else 1_000_000_000,
                                allow_zero=region.label == "할인") is None:
                    conflicting = True
                    break
                readings.append(token)
        # 다른 여백에서 검출 자체가 실패한 것은 상충하는 숫자가 아니다. 고신뢰도 이견은 모두 거절한다.
        if readings and not conflicting and len({token.text.strip() for token in readings}) == 1:
            result.append(Token(readings[0].text.strip(), min(region.confidence, *(token.confidence for token in readings)),
                                region.box, recovered_numeric=True, numeric_column=region.label))
            # 할인 칸이 끝내 누락돼도 좌표로 확인한 수량·금액까지 버리지 않는다. 숫자 값은 바꾸지 않는다.
            for index, token in enumerate(tokens):
                if token is region.amount_token:
                    result[index] = replace(result[index], numeric_column="금액")
                    break
    return result
