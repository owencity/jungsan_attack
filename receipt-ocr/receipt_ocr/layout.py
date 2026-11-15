"""겹친 글자 상자는 명시된 표의 열과 실제 숫자로만 다시 연결한다."""
import re
from statistics import median

from .dictionary import Entry, normalize
from .model import Token
from .product import NUMERIC_TOKEN, UNIT_PATTERN, match_product, read_integer


NAME_HEADERS = {"품명", "품목", "상품명", "상품", "메뉴명", "메뉴"}
COLUMN_HEADERS = {"단가", "가격", "수량", "할인", "금액"}
CELL = re.compile(rf"{NUMERIC_TOKEN}\s*(?:원|{UNIT_PATTERN}|개)?", re.I)
HEADER = re.compile(r"^(?:품명|품목|상품명|상품|메뉴명|메뉴|단가|가격|수량|할인|금액)+$")
FOOTER = re.compile(r"^(?:합계|총계|총액|총금액|총주문액|결제|과세|부가세|부가가치세|면세|"
                    r"매출|받을|받은|거스름|현금|카드|신용|공급가액|판매금액|소계|취소|환불|반품|void|cancel)", re.I)
QUALIFIER = re.compile(r"무알|논알|비알코올|알코올프리|알콜프리|제로|0[.]0|토닉|미등록|미상|불명|"
                       r"취소|환불|반품|서비스|증정|할인|쿠폰|세트|소주잔|와인잔|병따개|"
                       r"non.?alcohol|alcohol.?free|unknown|void|cancel|"
                       r"(?:^|[\s(])(?:병|캔|잔|피처|피쳐|bottles?|cans?|glass(?:es)?|pitcher)(?:$|[\s)])", re.I)


def _center(token: Token) -> tuple[float, float]:
    return ((token.box[0] + token.box[2]) / 2, (token.box[1] + token.box[3]) / 2)


def _header_cells(anchor, tokens, minimum_confidence, supported_columns):
    if anchor.confidence < minimum_confidence:
        return None
    cells = []
    for token in tokens:
        label = normalize(token.text)
        if label not in COLUMN_HEADERS:
            continue
        height = max(anchor.box[3] - anchor.box[1], token.box[3] - token.box[1])
        if abs(_center(token)[1] - _center(anchor)[1]) <= .65 * height and token.box[0] > anchor.box[2]:
            cells.append(("단가" if label == "가격" else label, token))
    cells.sort(key=lambda cell: cell[1].box[0])
    labels = [label for label, unused_token in cells]
    if (len(labels) != len(set(labels)) or set(labels) not in supported_columns
            or any(token.confidence < minimum_confidence for unused_label, token in cells)):
        return None
    centers = [_center(anchor)[0], *[_center(token)[0] for unused_label, token in cells]]
    limits = [(centers[index] + centers[index + 1]) / 2 for index in range(len(cells))]
    limits.append(centers[-1] + (centers[-1] - centers[-2]) / 2)
    return cells, limits


def _previous_item_has_values(previous, current, tokens, cells, limits, current_numbers):
    labels = [label for label, unused_token in cells]
    required = {"금액", "단가" if "단가" in labels else "수량"}
    previous_centers = []
    for index, label in enumerate(labels):
        if label not in required:
            continue
        values = [token for token in tokens if CELL.fullmatch(token.text.strip())
                  and limits[index] < token.box[0] < token.box[2] < limits[index + 1]
                  and _center(previous)[1] <= _center(token)[1] < _center(current)[1]]
        if len(values) != 1:
            return False
        pattern = rf"({NUMERIC_TOKEN})\s*(?:{UNIT_PATTERN}|개)?" if label == "수량" else rf"({NUMERIC_TOKEN})\s*원?"
        matched = re.fullmatch(pattern, values[0].text.strip(), re.I)
        if not matched or read_integer(matched[1], 1000 if label == "수량" else 1_000_000_000) is None:
            return False
        previous_centers.append(_center(values[0])[1])
    previous_offset = median(previous_centers) - _center(previous)[1]
    current_offset = median(_center(token)[1] for token in current_numbers) - _center(current)[1]
    # 앞 금액이 보인다는 이유만으로 이름 한정자를 버리지 않는다. 두 행의 기울기도 일치해야 한다.
    # 글자 높이 1/4 제한은 맞는 행을 더 거절할 수 있지만 모호한 행을 긍정으로 넓히지 않는다.
    tolerance = .25 * min(previous.box[3] - previous.box[1], current.box[3] - current.box[1])
    return abs(previous_offset - current_offset) <= tolerance


def reconstruct_item_rows(tokens: list[Token], rows: list[list[Token]], dictionary: dict[str, Entry],
                          minimum_confidence: float, supported_columns) -> list[list[Token]]:
    anchors = [token for token in tokens if normalize(token.text) in NAME_HEADERS]
    original_rows = {id(token): row for row in rows for token in row}
    candidates = []
    for name in tokens:
        product = match_product(name.text, dictionary)
        if (product is None or product.entry.category in {"NON_ALCOHOL", "NON_BEVERAGE"}
                or name.confidence < minimum_confidence):
            continue
        preceding = [anchor for anchor in anchors if _center(anchor)[1] < name.box[1]]
        if not preceding:
            continue
        anchor = max(preceding, key=lambda token: _center(token)[1])
        header = _header_cells(anchor, tokens, minimum_confidence, supported_columns)
        if header is None:
            continue
        cells, limits = header
        header_tokens = [anchor, *[token for unused_label, token in cells]]
        header_ids = {id(token) for token in header_tokens}
        header_bottom = max(_center(token)[1] for token in header_tokens)
        # 불완전한 다음 헤더나 결제/세금 영역에서 이전 표의 열을 재사용하지 않는다.
        if any(id(token) not in header_ids and header_bottom < _center(token)[1] <= _center(name)[1]
               and (HEADER.fullmatch(normalize(token.text)) or FOOTER.match(normalize(token.text)))
               for token in tokens):
            continue
        old_row = original_rows[id(name)]
        if (len(old_row) == len(cells) + 1
                and all(sum(CELL.fullmatch(token.text.strip()) is not None
                            and limits[index] < token.box[0] < token.box[2] < limits[index + 1]
                            for token in old_row) == 1 for index in range(len(cells)))):
            continue
        left_tolerance = 2 * max(name.box[3] - name.box[1], anchor.box[3] - anchor.box[1])
        if name.box[0] < anchor.box[0] - left_tolerance or name.box[2] >= limits[0]:
            continue
        height = name.box[3] - name.box[1]
        bottom = _center(name)[1] + 1.25 * height
        numbers = []
        for index, (unused_label, unused_header_token) in enumerate(cells):
            found = [token for token in tokens if id(token) not in header_ids and token is not name
                     and CELL.fullmatch(token.text.strip())
                     and limits[index] < token.box[0] < token.box[2] < limits[index + 1]
                     and _center(name)[1] <= _center(token)[1] <= bottom]
            if len(found) != 1:
                break
            numbers.append(found[0])
        if len(numbers) != len(cells):
            continue
        item_bottom = max(_center(token)[1] for token in numbers)
        blockers = []
        for token in tokens:
            value = token.text.strip()
            # 이름 열의 0.0 같은 숫자도 제품 한정자다. 분명한 금액 표기만 아래줄 가격으로 제외한다.
            left_price = (("," in value or value.endswith("원"))
                          and read_integer(value.removesuffix("원").strip(), 1_000_000_000) is not None)
            ordinary_cell = CELL.fullmatch(value) and (token.box[0] >= limits[0] or left_price)
            if (id(token) in header_ids or token is name or ordinary_cell
                    or not name.box[0] - height <= _center(token)[0] <= limits[-1]
                    or not max(header_bottom, _center(name)[1] - height) < _center(token)[1] <= item_bottom + .25 * height):
                continue
            # 긴 앞 상품명 상자는 아래 행과 겹쳐도 그 상품의 금액이 따로 보이면 앞 행이다.
            earlier = _center(token)[1] < _center(name)[1] - .25 * min(height, token.box[3] - token.box[1])
            combined = match_product(name.text + " " + token.text, dictionary)
            modifier = QUALIFIER.search(token.text) or (combined is not None and combined.entry != product.entry)
            ambiguous_name_number = CELL.fullmatch(value) and token.box[0] < limits[0] and not left_price
            if (ambiguous_name_number or modifier
                    or not (earlier and _previous_item_has_values(token, name, tokens, cells, limits, numbers))):
                blockers.append(token)
        if blockers:
            continue
        candidates.append((header_tokens, [name, *numbers]))
    # 한 숫자가 두 상품에 동시에 붙는 경우에는 가까운 쪽을 임의로 고르지 않는다.
    counts = {}
    for unused_header, item in candidates:
        for token in item:
            counts[id(token)] = counts.get(id(token), 0) + 1
    accepted = [(header, item) for header, item in candidates if all(counts[id(token)] == 1 for token in item)]
    if not accepted:
        return rows
    consumed = {id(token) for header, item in accepted for token in [*header, *item]}
    rebuilt = [[token for token in row if id(token) not in consumed] for row in rows]
    rebuilt = [row for row in rebuilt if row]
    emitted_headers = set()
    for header, item in accepted:
        identity = tuple(id(token) for token in header)
        if identity not in emitted_headers:
            rebuilt.append(header)
            emitted_headers.add(identity)
        rebuilt.append(item)
    return sorted(rebuilt, key=lambda row: median(_center(token)[1] for token in row))
