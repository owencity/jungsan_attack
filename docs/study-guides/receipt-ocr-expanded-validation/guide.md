# 추가 원본 검증 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 후속이다.
base `57d1ee955e890ce31c6d1f9eec361e130988dec9` → source head `069cf4307057393e2a31531432f6d7eee2e63bef`. 저장 커밋은 소스 head와 구분한다.
[정확한 diff](changes.diff.patch) · [검증 manifest](manifest.json).
정책·카탈로그 기준 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`. Python 변경이므로 Java/Kotlin 교재 목차에 연결하지 않는다.

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

#### Python · 좌표 기반 OCR 후처리

- 세로 구간의 교집합 — [extract.py:33](https://github.com/owencity/jungsan_attack/blob/069cf4307057393e2a31531432f6d7eee2e63bef/receipt-ocr/receipt_ocr/extract.py#L33): 서로 높이가 다른 상자의 중심 거리 대신 짧은 상자 높이 대비 겹침이 줄 연결 경계를 바꾸기 때문.
- 경계값·입력 보존 — [test_public_layouts.py:47](https://github.com/owencity/jungsan_attack/blob/069cf4307057393e2a31531432f6d7eee2e63bef/receipt-ocr/tests/test_public_layouts.py#L47): 절반 겹침과 그 미만이 다른 줄이 되며 입력 순서를 바꾸지 않는지 확인하기 때문.
- 실패 격리 — [test_public_layouts.py:33](https://github.com/owencity/jungsan_attack/blob/069cf4307057393e2a31531432f6d7eee2e63bef/receipt-ocr/tests/test_public_layouts.py#L33): 총액 누락을 세금 줄의 숫자로 채우지 않는지 직접 확인하기 때문.

#### JSON · 서비스 계약

- null과 확인 사유 보존 — [test_public_layouts.py:68](https://github.com/owencity/jungsan_attack/blob/069cf4307057393e2a31531432f6d7eee2e63bef/receipt-ocr/tests/test_public_layouts.py#L68): 인계 JSON과 실제 반환의 일치가 미상 수량을 0으로 바꾸는 연결 오류를 막는 근거이기 때문.
- 전체 정답 평가 — [정답 manifest](https://github.com/owencity/jungsan_attack/blob/069cf4307057393e2a31531432f6d7eee2e63bef/receipt-ocr/examples/manifest.expanded-public.example.json): 읽지 못한 수량 1도 정답에 남겨 총액 2/2와 주류 전체 필드 1/2를 구분하기 때문.

## 실행한 검증과 남은 범위

98 tests/OK, server compile 성공. 실제 모델의 ASGI 업로드 4건 200·health 200·키 없음 401.
정답 2장은 개발 사진이며 독립 표본 정확도가 아니다. 새 사진 수량 누락·결제 전표·복수 문서의 주류 누락은 남는다.
Windows PC 지연은 운영/ARM 검증을 대신하지 않는다. Spring/앱·TCP·운영 연결은 아직 하지 않았다.
자체 점검만 수행했으며 타인 리뷰·병합·학습 완료를 주장하지 않는다.
