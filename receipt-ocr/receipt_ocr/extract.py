import re
from .dictionary import Entry
from .model import Drink, Receipt, Token
from .product import INTEGER, MAX_QUANTITY, NUMERIC_TOKEN, UNIT_NAMES, UNIT_PATTERN, match_product, read_integer


MAX_AMOUNT = 1_000_000_000
MIN_CONFIDENCE = 0.85
VALUE_WITH_UNIT = re.compile(rf"({NUMERIC_TOKEN})\s*(원|{UNIT_PATTERN})?", re.I)
VALUES = re.compile(rf"{NUMERIC_TOKEN}\s*(?:원|{UNIT_PATTERN})?(?:\s+{NUMERIC_TOKEN}\s*(?:원|{UNIT_PATTERN})?){{0,2}}", re.I)
TOTAL = re.compile(rf"^(합계금액|합계|총액|총금액|결제금액|총결제금액|받을금액)\s*[:：]?\s*({INTEGER})\s*원?$")
HEADER_LABELS = re.compile(r"단가|수량|할인|금액")
HEADER_TEXT = re.compile(r"^(?:품명|품목|상품명|메뉴명|메뉴|단가|수량|할인|금액)+$")
SUPPORTED_COLUMNS = ({"단가", "수량", "금액"}, {"수량", "금액"}, {"수량", "할인", "금액"})
SPECIAL_ROW = re.compile(r"취소|환불|반품|VOID|CANCEL|할인|쿠폰|서비스|세트|[0-9]+\s*\+\s*[0-9]+", re.I)


def split_product_values(text: str, dictionary: dict[str, Entry]):
    # 오른쪽 숫자를 떼더라도 카스 0.0 같은 더 긴 전체 상품명을 우선한다.
    product = match_product(text, dictionary)
    if product:
        return product, []
    for separator in reversed(list(re.finditer(r"\s+", text))):
        values_text = text[separator.end():].strip()
        if not VALUES.fullmatch(values_text):
            continue
        product = match_product(text[:separator.start()], dictionary)
        if product:
            return product, [(match[1], match[2]) for match in VALUE_WITH_UNIT.finditer(values_text)]
    return None, []


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
    rows = group_rows(tokens)
    # 가로로 붙은 두 표를 한 주문으로 섞지 않는다. 문서 분할이나 누락된 총액을 추측하지 않는다.
    for row in rows:
        normalized = re.sub(r"\s+", "", "".join(token.text for token in row))
        labels = HEADER_LABELS.findall(normalized)
        if HEADER_TEXT.fullmatch(normalized) and labels.count("수량") > 1 and labels.count("금액") > 1:
            return Receipt(None, [], ["MULTIPLE_RECEIPTS_SUSPECTED"])
    for row in rows:
        text = " ".join(token.text.strip() for token in row).strip()
        confidence = min(token.confidence for token in row)
        normalized_text = re.sub(r"\s+", "", text)
        total_match = TOTAL.fullmatch(normalized_text)
        if total_match:
            amount = read_integer(total_match[2], MAX_AMOUNT, allow_zero=True)
            if amount is not None and confidence >= MIN_CONFIDENCE:
                totals.append(amount)
            else:
                warnings.append("TOTAL_NEEDS_REVIEW")
            continue
        labels = HEADER_LABELS.findall(normalized_text)
        if labels and HEADER_TEXT.fullmatch(normalized_text):
            columns = labels if (len(labels) == len(set(labels)) and set(labels) in SUPPORTED_COLUMNS
                                 and confidence >= MIN_CONFIDENCE) else None
            continue
        product, values = split_product_values(text, dictionary)
        if product is None or product.entry.category in {"NON_ALCOHOL", "NON_BEVERAGE"}:
            continue
        reasons = list(product.reasons)
        if any(token.recovered_numeric for token in row):
            reasons.append("NUMERIC_REGION_RECOGNIZED")
        unit = product.unit
        quantity = product.quantity
        if sum(token.recovered_numeric and token.numeric_column == "수량" for token in row) > 1:
            quantity = None
            reasons.append("QUANTITY_INVALID_OR_CONFLICT")
        unit_price = line_amount = None
        if SPECIAL_ROW.search(text):
            reasons.append("RETURN_OR_CANCEL" if re.search(r"취소|환불|반품|VOID|CANCEL", text, re.I) else "COMBO_OR_PROMOTION")
            quantity = None
        if columns is None:
            reasons.append("COLUMN_HEADER_MISSING")
            # 단위가 붙은 하나의 수량은 헤더 없이도 읽는다. 나머지 가격 열은 추측하지 않는다.
            if len(values) == 1 and values[0][1] and values[0][1].casefold() in UNIT_NAMES:
                quantity = read_integer(values[0][0], MAX_QUANTITY)
                receipt_unit = UNIT_NAMES[values[0][1].casefold()]
                if unit != "UNKNOWN" and unit != receipt_unit:
                    reasons.append("UNIT_CONFLICT")
                    unit = "UNKNOWN"
                else:
                    unit = receipt_unit
                if quantity is None:
                    reasons.append("QUANTITY_INVALID_OR_CONFLICT")
        elif len(values) != len(columns):
            reasons.append("ITEM_VALUES_MISSING")
            # 재인식이 확인한 열만 사용한다. 남은 숫자의 개수나 가격으로 빈 할인·단가를 추측하지 않는다.
            recovered_quantities = [token for token in row if token.recovered_numeric and token.numeric_column == "수량"]
            aligned_amounts = [token for token in row if token.numeric_column == "금액"]
            if len(aligned_amounts) == 1:
                line_amount = read_integer(re.sub(r"\s+|원$", "", aligned_amounts[0].text), MAX_AMOUNT)
            if len(recovered_quantities) == 1:
                aligned_quantity = read_integer(recovered_quantities[0].text.strip(), MAX_QUANTITY)
                if quantity is not None and quantity != aligned_quantity:
                    reasons.append("QUANTITY_INVALID_OR_CONFLICT")
                    quantity = None
                elif "QUANTITY_INVALID_OR_CONFLICT" not in reasons:
                    quantity = aligned_quantity
            if aligned_amounts and line_amount is None:
                quantity = None
                reasons.append("ITEM_OUT_OF_RANGE")
            if (aligned_amounts or recovered_quantities) and "단가" not in columns:
                reasons.append("ITEM_UNIT_PRICE_MISSING")
        else:
            mapped = dict(zip(columns, values))
            parsed = {label: read_integer(value, MAX_QUANTITY if label == "수량" else MAX_AMOUNT,
                                          allow_zero=label == "할인")
                      for label, (value, unused_unit) in mapped.items()}
            value_units = {label: value_unit.casefold() if value_unit else None
                           for label, (unused_value, value_unit) in mapped.items()}
            valid_units = all(value_unit in (None, "원") for label, value_unit in value_units.items() if label != "수량")
            valid_units = valid_units and value_units["수량"] in (None, *UNIT_NAMES)
            if not valid_units or any(value is None for value in parsed.values()):
                reasons.append("ITEM_OUT_OF_RANGE")
                unit_price = quantity = line_amount = None
            else:
                unit_price, column_quantity, line_amount = parsed.get("단가"), parsed["수량"], parsed["금액"]
                if unit_price is None:
                    reasons.append("ITEM_UNIT_PRICE_MISSING")
                if parsed.get("할인", 0) > 0:
                    reasons.append("ITEM_DISCOUNT_PRESENT")
                if quantity is not None and quantity != column_quantity:
                    reasons.append("QUANTITY_INVALID_OR_CONFLICT")
                    quantity = None
                elif "QUANTITY_INVALID_OR_CONFLICT" not in reasons:
                    quantity = column_quantity
                receipt_unit = UNIT_NAMES.get(value_units["수량"])
                if receipt_unit:
                    if "UNIT_CONFLICT" in reasons or (unit != "UNKNOWN" and unit != receipt_unit):
                        reasons.append("UNIT_CONFLICT")
                        unit = "UNKNOWN"
                    else:
                        unit = receipt_unit
                if quantity is not None and unit_price is not None and unit_price * quantity != line_amount:
                    reasons.append("ITEM_AMOUNT_MISMATCH")
        if product.entry.serving_type == "DRAFT" and unit == "BOTTLE":
            reasons.append("UNIT_CONFLICT")
            unit = "UNKNOWN"
        if "RETURN_OR_CANCEL" in reasons or ("COMBO_OR_PROMOTION" in reasons and unit != "PACK"):
            quantity = None
        if confidence < MIN_CONFIDENCE:
            reasons.append("LOW_CONFIDENCE")
        if unit == "UNKNOWN":
            reasons.append("UNKNOWN_CONTAINER")
        elif unit != "BOTTLE":
            reasons.append("UNIT_REQUIRES_MAPPING")
        if product.entry.category == "UNKNOWN":
            reasons.append("UNKNOWN_CATEGORY")
        drinks.append(Drink(product.entry.name, product.entry.category, unit, unit_price, quantity,
                            line_amount, round(confidence, 4), list(dict.fromkeys(reasons)),
                            product.entry.serving_type, product.volume_ml))
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
