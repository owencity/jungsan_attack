import re
from .dictionary import Entry, normalize
from .model import Drink, Receipt, Token


MAX_AMOUNT = 1_000_000_000
MAX_QUANTITY = 1000
MIN_CONFIDENCE = 0.85
# 정수 원만 지원한다. 5.000·5,00·음수·소수는 5000으로 복구하지 않는다.
NUMBER = r"(?:\d{1,3}(?:,\d{3})+|\d+)"
ROW_VALUES = re.compile(
    rf"^(?P<name>.+?)\s+(?P<values>{NUMBER}(?:\s*(?:원|병|캔|잔))?"
    rf"(?:\s+{NUMBER}(?:\s*(?:원|병|캔|잔))?){{0,2}})\s*$"
)
VALUE = re.compile(rf"(?<![\d,.+-]){NUMBER}(?![\d,.])")
VALUE_WITH_UNIT = re.compile(rf"({NUMBER})\s*(원|병|캔|잔)?")
TOTAL = re.compile(rf"^(합계|총액|총금액|결제금액|총결제금액|받을금액)\s*[:：]?\s*({NUMBER})\s*원?$")
HEADER_LABELS = re.compile(r"단가|수량|금액")


def group_rows(tokens: list[Token]) -> list[list[Token]]:
    rows: list[list[Token]] = []
    for token in sorted(tokens, key=lambda value: ((value.box[1] + value.box[3]) / 2, value.box[0])):
        center = (token.box[1] + token.box[3]) / 2
        height = token.box[3] - token.box[1]
        if rows:
            anchor = rows[-1][0]
            anchor_center = (anchor.box[1] + anchor.box[3]) / 2
            if abs(center - anchor_center) <= 0.5 * min(height, anchor.box[3] - anchor.box[1]):
                rows[-1].append(token)
                continue
        rows.append([token])
    return [sorted(row, key=lambda token: token.box[0]) for row in rows]


def extract(tokens: list[Token], dictionary: dict[str, Entry]) -> Receipt:
    warnings = []
    drinks = []
    totals = []
    columns = None
    for row in group_rows(tokens):
        text = " ".join(token.text.strip() for token in row).strip()
        confidence = min(token.confidence for token in row)
        normalized_text = re.sub(r"\s+", "", text)
        total_match = TOTAL.fullmatch(normalized_text)
        if total_match:
            amount = int(total_match[2].replace(",", ""))
            if amount <= MAX_AMOUNT and confidence >= MIN_CONFIDENCE:
                totals.append(amount)
            else:
                warnings.append("TOTAL_NEEDS_REVIEW")
            continue
        labels = HEADER_LABELS.findall(text)
        if sorted(labels) == ["금액", "단가", "수량"]:
            columns = labels if confidence >= MIN_CONFIDENCE else None
            continue
        row_match = ROW_VALUES.fullmatch(text)
        # 숫자가 제품명(500cc·0.0)에 들어갈 수 있으므로 이름 전체로 사전을 조회한다.
        name = row_match["name"] if row_match else text
        entry = dictionary.get(normalize(name))
        if entry is None or entry.category == "NON_ALCOHOL":
            continue
        values = VALUE.findall(row_match["values"]) if row_match else []
        reasons = []
        unit = entry.unit
        unit_price = quantity = line_amount = None
        if columns is None:
            reasons.append("COLUMN_HEADER_MISSING")
        elif len(values) != 3:
            reasons.append("ITEM_VALUES_MISSING")
        else:
            mapped = dict(zip(columns, (int(value.replace(",", "")) for value in values)))
            unit_price, quantity, line_amount = mapped["단가"], mapped["수량"], mapped["금액"]
            value_units = dict(zip(columns, (match[2] for match in VALUE_WITH_UNIT.finditer(row_match["values"]))))
            receipt_unit = {"병": "BOTTLE", "캔": "CAN", "잔": "GLASS"}.get(value_units["수량"])
            if receipt_unit and receipt_unit != entry.unit:
                unit = receipt_unit
                reasons.append("UNIT_CONFLICT")
            if not (0 < unit_price <= MAX_AMOUNT and 0 < quantity <= MAX_QUANTITY and 0 < line_amount <= MAX_AMOUNT):
                reasons.append("ITEM_OUT_OF_RANGE")
                unit_price = quantity = line_amount = None
            elif unit_price * quantity != line_amount:
                reasons.append("ITEM_AMOUNT_MISMATCH")
        if confidence < MIN_CONFIDENCE:
            reasons.append("LOW_CONFIDENCE")
        if unit != "BOTTLE":
            reasons.append("UNIT_REQUIRES_MAPPING")
        drinks.append(Drink(entry.name, entry.category, unit, unit_price, quantity,
                            line_amount, round(confidence, 4), reasons))
    if not tokens:
        warnings.append("OCR_NO_TEXT")
    if len(set(totals)) > 1:
        total = None
        warnings.append("TOTAL_CONFLICT")
    else:
        total = totals[0] if totals else None
    if total is None and "TOTAL_CONFLICT" not in warnings:
        warnings.append("TOTAL_NOT_FOUND")
    if not drinks:
        warnings.append("NO_DRINK_ITEMS")
    if any(drink.review_reasons for drink in drinks):
        warnings.append("ITEMS_NEED_REVIEW")
    # 음식값이 포함되므로 술값과 총액의 동등 비교는 하지 않는다. 초과만 모순이다.
    amounts = [drink.line_amount for drink in drinks]
    if total is not None and all(amount is not None for amount in amounts):
        if sum(amounts) > total:
            warnings.append("DRINK_AMOUNT_EXCEEDS_TOTAL")
    return Receipt(total, drinks, list(dict.fromkeys(warnings)))
