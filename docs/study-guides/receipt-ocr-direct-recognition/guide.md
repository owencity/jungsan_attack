# 숫자 직접 인식 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속이다.
base `dbcc997884c6fcf62cc218554237333a614b9025` → source head `f928e21eb42b21a6709b1703b6bab819073ad246`. 메타데이터 저장 커밋과 소스 head를 구분한다.
[정확한 diff](changes.diff.patch) · [검증 manifest](manifest.json).
정책·카탈로그 기준 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`. Python 변경을 Java/Kotlin 교재 항목에 연결하지 않는다.

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

- 숫자 행 중심의 중앙값과 인접 행 경계 — [numeric_regions.py:63](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/receipt_ocr/numeric_regions.py#L63): 원근 사진의 앞줄 잉크를 제외하면서 대상 숫자 여백을 보존하는 범위가 재인식 여부를 결정하기 때문.
- 빈 칸 선별과 지연 평가 — [numeric_regions.py:63](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/receipt_ocr/numeric_regions.py#L63): 기존 토큰이 있는 칸은 제외하고 최대 8칸만 추가 인식해 추가 작업을 제한하기 때문.
- 원근 경계 회귀 — [test_numeric_regions.py:262](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/tests/test_numeric_regions.py#L262): 금액 중심 하나로 자를 때 생기던 대상 숫자 잘림을 실제 좌표 관계로 재현하기 때문.

#### PaddleOCR · 공개 모듈 경계

- 위치 검출과 글자 인식 분리 — [ocr.py:33](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/receipt_ocr/ocr.py#L33): 위치를 아는 숫자 칸을 TextRecognition에 직접 넣어 전체 검출을 반복하지 않기 때문.
- 모델명 고정과 기동 실패 — [test_ocr_adapter.py:12](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/tests/test_ocr_adapter.py#L12): 공개 모듈의 기본 모델 변경에 기대지 않고 한국어 가중치와 CPU 옵션을 명시하기 때문.
- 인식 결과의 경계 검증 — [test_ocr_adapter.py:49](https://github.com/owencity/jungsan_attack/blob/f928e21eb42b21a6709b1703b6bab819073ad246/receipt-ocr/tests/test_ocr_adapter.py#L49): 네이티브 출력의 비정상 점수를 신뢰할 수 있는 숫자로 바꾸지 않아야 하기 때문.

## 검증과 미완료

106 tests/OK, server compile 성공. 실제 원본/파생 대조 8건, 실제 모델 ASGI 업로드 8건을 확인했다.
전체 정답 2장의 주류 9필드 일치가 1/2에서 2/2로 바뀌었으며 같은 개발 사진이다. 일반 정확도·독립 평가가 아니다.
별도 인식 인스턴스가 메모리를 더 쓰고 CPU 지연 편차가 크다. ARM·대표 새 매장 표본·Spring/앱·운영 연결은 미검증이다.
자체 점검만 수행했으며 타인 리뷰·병합·배포·사용자 학습 완료를 주장하지 않는다.
