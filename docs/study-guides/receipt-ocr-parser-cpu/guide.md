# 영수증 파서와 CPU 비교 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속이다.
base `4e33613454df2afd9407b1bc1fac3622af572683` → source head `5fa7c8d4dc93305307561ac3c27f5d959838e4a7`. 정책·카탈로그 기준 `8d27496270e6c8adcb78b4580d68c150f9c3cce6`.
문서 보존 커밋과 코드 source head를 구분한다. [정확한 diff](changes.diff.patch) · [검증 manifest](manifest.json).

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

- 정규식의 전체 일치와 금액 의미 경계 — [extract.py:11](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/receipt_ocr/extract.py#L11): 새 합계 표기를 지원하면서 세전 판매금액·숫자 일부를 총액으로 채택하지 않는 이유를 확인한다.
- 할인 문맥의 미확정 값 — [extract.py:92](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/receipt_ocr/extract.py#L92): 읽지 못한 할인과 명시적 0이 서로 다른 상태로 처리되는 지점을 확인한다.
- 좌표와 열 개수로 제한한 인접 행 연결 — [extract.py:49](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/receipt_ocr/extract.py#L49): 상품코드를 수량으로 읽거나 다른 상품의 숫자를 빌리지 않는 조건을 확인한다.
#### PaddleOCR · CPU 측정

- 검출 해상도와 CPU 비용 — [ocr.py:22](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/receipt_ocr/ocr.py#L22): 원본 해상도와 검출 입력 크기를 구분하고 작은 글자 인식의 회귀를 대조하는 이유를 확인한다.
- 네이티브 프로세스의 시간 제한과 부분 실패 — [benchmark_cpu.py:124](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/tools/benchmark_cpu.py#L124): 실행 중 오류·시간 초과가 관측과 분모에서 사라지지 않는 경계를 확인한다.
- 소스·정답 고정과 반복 측정 — [benchmark_cpu.py:91](https://github.com/owencity/jungsan_attack/blob/5fa7c8d4dc93305307561ac3c27f5d959838e4a7/receipt-ocr/tools/benchmark_cpu.py#L91): 설정 비교 중 입력과 판정 로직이 바뀌면 비교를 중단하는 이유를 확인한다.

## 검증과 미완료

실제 모델 결과·부분 정답 범위·시간은 [개선 보고서](../../../receipt-ocr/docs/accuracy-cpu-2026-11-04.md)를 따른다.
13장은 수정에 사용한 개발 대조군이다. 두 기존 사진만 주류 9필드 정답이며, 나머지는 부분 정답과 부정 대조다.
일반 정확도·ARM 운영·동시 부하·앱 체감 시간·외부 리뷰·사용자 학습·병합·배포 완료와 구분한다.
