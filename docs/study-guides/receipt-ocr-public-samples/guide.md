# 공개 영수증 양식 보완 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 추가 구현만 고정했다.
base `8269004454e3ce403d7a9dcaf554d3bf06ff64a7` → source head `9a0a8e81149861f654d051f9959885cb81a74c82`. 이 가이드의 저장 커밋은 별도다.
[정확한 diff](changes.diff.patch) · [기준/검증 정보](manifest.json).
원격 main의 정책·카탈로그 기준은 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`다. Python 코드를 Java/Kotlin 교재에 억지 대응하지 않는다.

### Java 기본기

- 해당 없음

### Effective Java

- 해당 없음

### 함수형 Java

- 해당 없음

### Kotlin 기본

- 해당 없음

### Kotlin 고급

- 해당 없음

### 기술별 학습 포인트

#### Python 추출 경계

- 명시 헤더와 누락 값 — [extract.py:48](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/receipt_ocr/extract.py#L48) `extract`: 단가 없는 표의 실제 수량/금액과 역산으로 만든 값을 구별하는 지점.
- 복수 표의 조기 반환 — [extract.py:57](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/receipt_ocr/extract.py#L57) `extract`: 가로로 중복된 헤더를 읽었을 때 서로 다른 주문을 합치지 않는 분기.
- 전체 이름을 보존하는 제한적 조합 — [product.py:70](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/receipt_ocr/product.py#L70) `_qualified_draft_entry`: 정확한 BEER 브랜드와 일반 생맥 표기만 조합하는 경계.

#### OCR 평가와 회귀

- 숫자 누락을 정답으로 메우지 않기 — [test_public_layouts.py:39](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/tests/test_public_layouts.py#L39) `test_ocr_missing_small_quantity_and_zero_never_defaults_to_one`: 실제 인식 실패와 파서 정책 통과를 구분하는 기대값.
- 비알코올 접미어 보호 — [test_public_layouts.py:75](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/tests/test_public_layouts.py#L75) `test_parenthesized_name_keeps_non_alcohol_and_unknown_qualifiers_safe`: 새 이름 조합 때문에 제외 제품이 술로 바뀌지 않는 회귀 경계.
- 고정 정답과 실패 보존 — [manifest.public-web.example.json:1](https://github.com/owencity/jungsan_attack/blob/9a0a8e81149861f654d051f9959885cb81a74c82/receipt-ocr/examples/manifest.public-web.example.json#L1): 읽히지 않은 수량을 null 정답으로 바꿔 평가를 통과시키지 않는 기준.

## 검증과 미완료

72개 단위/API 테스트와 서버 컴파일은 통과했다. 공개 웹 사례는 수정에 사용한 작은 개발 표본이다.
완전한 정답 1장에서 총액 1/1, 주류 전체 필드 0/1이다. 정확도 목표 달성이나 독립 평가가 아니다.
자체 점검만 했으며 타인 리뷰·사용자 학습 완료·병합·운영 적용은 별도다.
[출처·실측·숫자 누락 한계](../../../receipt-ocr/docs/public-sample-validation.md)를 함께 본다.
