# 사용자 영수증 사전 반영 — Python 코드 기반 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속 구현을 고정했다.
구현 base: `a37d257337b500ce7dd4c0cf73f626963c1395f4`, 구현 head: `d10b73b947c7af38536f458b77728c052c9758ed`. 이 안내를 추가한 커밋과 구현 head는 다르다.
오프라인 변경 코드: [changes.diff.patch](changes.diff.patch), 기준 정보: [manifest.json](manifest.json).
원격 main 정책·카탈로그 기준은 `3eb5254`이며 Java/Kotlin 교재 목차를 Python 구현에 임의 대응하지 않는다.

## 읽는 순서

- [dictionary.py:33](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/dictionary.py#L33) — 사전에도 브랜드 용기 추측을 허용하지 않는 경계 검증.
- [product.py:29](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/product.py#L29) — 숫자 전체를 검증해 소수·음수·잘못된 쉼표를 양수로 구제하지 않기.
- [product.py:40](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/product.py#L40) — Fraction으로 L→mL을 정확히 변환하고 정수 mL 지원 범위 검사.
- [product.py:69](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/product.py#L69) — 긴 제외 이름을 보호하고 명시 용량·단위·수량을 독립 파싱.
- [extract.py:16](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/extract.py#L16) — 카스 0.0을 카스와 수량 0으로 나누지 않게 하는 상품명 경계.
- [model.py:34](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/receipt_ocr/model.py#L34) — null/0/양수의 의미, 단위 충돌·낮은 신뢰도가 병 수에 미치는 영향.
- [test_policy.py:22](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/tests/test_policy.py#L22) — 상품 존재와 실제 영수증 용기 근거를 구분하는 회귀 테스트.
- [test_evaluate.py:35](https://github.com/owencity/jungsan_attack/blob/d10b73b947c7af38536f458b77728c052c9758ed/receipt-ocr/tests/test_evaluate.py#L35) — 수량만 맞고 용량·병 수가 틀린 경우 정확도 지표에 반영하기.

## 검증과 한계

Python 단위/API 테스트 61건 통과. 실제 PaddleOCR + FastAPI 합성 검사 HTTP 200.
사전의 제품 존재가 영수증 인식 정확도를 보장하지 않고, null을 0으로 바꾸면 누락을 숨기게 된다.
상세 결과는 [VALIDATION](../../../receipt-ocr/VALIDATION.md)을 따른다. 위 링크는 구현 head에 고정돼 있다.
실제 영수증·매장별 양식·ARM·Spring 연결·독립 리뷰·운영 적용은 후속이다.
