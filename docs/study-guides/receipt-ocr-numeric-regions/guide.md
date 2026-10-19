# 작은 숫자 재인식 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속 구현이다.
base `335cd93c4acaa9be3c22d5818ec6ed63b620a00d` → source head `c9cb316da4af5254d790b5b7db6860d1c57f830e`. 기록 저장 커밋과 소스 head는 구분한다.
[정확한 코드 diff](changes.diff.patch) · [기준과 검증](manifest.json).
정책·카탈로그 기준은 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`이며 Python 구현을 Java/Kotlin 교재 목차에 억지로 연결하지 않는다.

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

#### Python · 이미지와 추론 경계

- 헤더 좌표와 후보 제한 — [numeric_regions.py:62](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/numeric_regions.py#L62) `_regions`: 인식한 금액 행과 빈 숫자 칸을 연결하며 기존 토큰을 덮지 않는 조건.
- 픽셀 검사와 이미지 수명 — [numeric_regions.py:99](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/numeric_regions.py#L99) `_prepare_cell`: 빈 칸/잘린 잉크/구분선을 거르고 생성 이미지의 자원을 닫는 경계.
- 유한한 추가 추론과 이견 — [numeric_regions.py:119](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/numeric_regions.py#L119) `recover_numeric_regions`: 8칸/16회 예산과 고신뢰도 숫자 상충을 거절하는 분기.
- 비재귀 모델 재사용 — [ocr.py:28](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/ocr.py#L28) `_recognize_once`: 확대 영역을 다시 전체 복구 경로에 넣지 않는 호출 분리.
- 불변 토큰의 내부 근거 — [model.py:6](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/model.py#L6) `Token`: 원래 숫자와 열 위치를 바꾸지 않고 재인식 여부/열 근거를 전달하는 경계.
- 부분 필드 보존 — [extract.py:48](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/receipt_ocr/extract.py#L48) `extract`: 빠진 할인/단가를 만들지 않고 좌표로 확인한 수량과 금액만 사용하는 분기.

#### 테스트와 평가

- 빈 칸의 오탐 방지 — [test_numeric_regions.py:63](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/tests/test_numeric_regions.py#L63): 숫자가 없을 때 모델을 부르거나 기본 1로 메우지 않는 기대값.
- 고신뢰도 이견 거절 — [test_numeric_regions.py:137](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/tests/test_numeric_regions.py#L137): 한 판독이 유효해도 다른 판독의 음수/소수/범위 위반을 무시하지 않는 조건.
- 부분 수량의 의미 — [test_numeric_regions.py:77](https://github.com/owencity/jungsan_attack/blob/c9cb316da4af5254d790b5b7db6860d1c57f830e/receipt-ocr/tests/test_numeric_regions.py#L77): 누락 할인과 확인된 수량/금액을 구분하는 기대값.

## 검증과 한계

92개 테스트·서버 컴파일 통과. 이전에 실패한 같은 1장에서 주류 9필드 일치가 0/1 → 1/1이다.
수량 지움·음수·JPEG 재압축은 같은 원본에서 만든 대조군이며 독립 사진 3장으로 세지 않는다.
[실제 결과·시간·한계](../../../receipt-ocr/docs/numeric-region-validation.md)를 함께 본다.
자체 점검·테스트·가이드 생성은 타인 리뷰·사용자 학습 완료·병합·운영 적용과 별개다.
