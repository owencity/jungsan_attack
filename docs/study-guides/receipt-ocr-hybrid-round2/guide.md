# 인식 단계 CPU 가속과 행 연결 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속이다.
base `2a97b7d50be97eba99bb2f20554e81178968a384` → source head `abdb5a449c58f27519d209caef40afc7f4be0cb6`. 정책·카탈로그 `8d27496270e6c8adcb78b4580d68c150f9c3cce6`.
[정확한 diff](changes.diff.patch) · [검증 manifest](manifest.json). 코드와 문서 보존 커밋을 구분한다.

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

#### Python · 좌표 기반 OCR

- 열 경계와 토큰 소유권 — [layout.py:73](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/receipt_ocr/layout.py#L73): 같은 숫자를 두 상품이나 옆 표에 붙이지 않는 조건을 확인한다.
- 이전 행 예외와 거절 조건 — [layout.py:48](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/receipt_ocr/layout.py#L48): 행 기울기가 다른 한정 문구를 앞 음식으로 버리지 않는 범위를 확인한다.
- 숫자형 상품 한정자 보존 — [layout.py:137](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/receipt_ocr/layout.py#L137): 0.0·부호·잘못된 쉼표를 수량 연결에서 버리지 않는 이유를 확인한다.

#### PaddleOCR · CPU 실행

- 모듈별 엔진 설정 — [ocr.py:31](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/receipt_ocr/ocr.py#L31): 오류가 나는 검출 경로와 시간이 집중된 인식 경로의 CPU 설정을 나누는 이유를 확인한다.
- 자동값과 명시적 거절값 — [ocr.py:20](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/receipt_ocr/ocr.py#L20): None·False를 구분해 x86 기본과 ARM의 기존 경로를 유지하는 조건을 확인한다.

#### 실행 측정 · 자원 정리

- 작업 범위의 절전 요청과 finally — [benchmark_cpu.py:35](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/tools/benchmark_cpu.py#L35): 장시간 이상치의 원인을 추정하지 않고 요청을 복구하며 비교하는 경계를 확인한다.
- 최고 RSS의 단위 — [benchmark_cpu.py:64](https://github.com/owencity/jungsan_attack/blob/abdb5a449c58f27519d209caef40afc7f4be0cb6/receipt-ocr/tools/benchmark_cpu.py#L64): 처리시간 개선과 함께 플랫폼별 메모리 비용을 기록하는 이유를 확인한다.

검증과 한계는 [보고서](../../../receipt-ocr/docs/round2-accuracy-hybrid-2026-11-04.md)를 따른다.
개발13개와 새3개를 구분한다. 새3개 전체 선택 조건0/3도 보존하고 대표 정확도로 설명하지 않는다.
자체·에이전트 점검은 외부 리뷰·사용자 학습·병합·배포 완료가 아니다.
