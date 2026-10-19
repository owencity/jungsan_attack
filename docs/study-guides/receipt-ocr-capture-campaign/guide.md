# 새 원본·촬영 조건 검증 — 고정 diff 학습 위치

PR [#34](https://github.com/owencity/jungsan_attack/pull/34)의 검증 후속이다.
base `524ee20cfc15e18fce276758114c2c5947cc25f2` → source head `d1c9b23736b76c7057e0cb8c5e6e9f3323b86275`. [고정 diff](changes.diff.patch) · [기준/검증](manifest.json).
정책·카탈로그는 `3e37cb54d50253ac185223e7b26aee0fbe08fffa`다. 문서 메타데이터 커밋과 소스 head를 구분한다.
운영 OCR·사전·API를 바꾼 작업이 아니며 Python 평가 코드를 Java/Kotlin 학습으로 꾸미지 않는다.

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

#### Python · 평가 기준과 원본 추적

- 부분 정답과 null/미존재 구별 — [compare_result:35](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/tools/validate_capture_campaign.py#L35): 명시한 필드만 대조하고 없는 필드를 null 정답으로 세지 않는 이유를 본다. 부분 검사 성공을 9필드 정확도로 부르지 않는다.
- 원본 SHA-256 검증과 실패 관측 — [inspect_case:54](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/tools/validate_capture_campaign.py#L54): 원본 교체를 OCR 전에 거절하고 파일 실패도 관측에서 빼지 않는 경로를 본다. SHA-256은 사진 저작권·대표성을 보장하지 않는다.
- 표본 묶음과 평가 누출 — [고정 manifest](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/examples/manifest.capture-campaign.example.json#L1): 같은 매장의 영수증/메뉴판을 같은 family로 두는 이유, 개발/새 도전/대조 구분을 본다. 원본 16개와 독립 매장 표본 16개는 다르다.
- 실패를 성공 종료로 숨기지 않기 — [main:76](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/tools/validate_capture_campaign.py#L76): 실행 예외와 인식 불일치를 따로 기록하되 둘 다 종료 코드에 반영한다. 기대값을 모델 출력으로 수정하지 않는다.
- 안전성 검증 — [CampaignTests:9](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/tests/test_campaign.py#L9): 누락/추가 행·부분 정답·가린 가격·변경된 원본·파일 오류의 기대 결과를 본다. 테스트 112개는 영수증 정확도 112건이 아니다.

#### OCR · 처리시간의 해석

- 단계별 단건 시간 — [TimedEngine:18](https://github.com/owencity/jungsan_attack/blob/d1c9b23736b76c7057e0cb8c5e6e9f3323b86275/receipt-ocr/tools/validate_capture_campaign.py#L18): 최초 전체 검출/인식과 추가 숫자 인식, 모델 초기화를 분리한다. 최초 단계 내부 검출/인식은 아직 분리 실측하지 않았다.
- 혼합 표본 통계의 한계 — [실측 결과](../../../receipt-ocr/docs/capture-campaign-validation.md): 중앙값 18.547초·범위 11.566~50.809초는 서로 다른 사진의 단건 결과다. 해상도/자르기 전후 개선율·ARM/운영 SLA가 아니다.

## 검증과 남은 일

112 tests/OK, 실제 모델 16개/실행 실패 0/기대 조건 일치 8·불일치 8, 숫자 총액 5/10.
새 총액은 1/5, 주류 행 누락과 메뉴판 후보 오인식도 보존했다. 보존 CLI의 실제 메뉴판 재실행도 불일치 종료 1이다.
총액 진단·메뉴판 오인식·여러 줄 주류·촬영/자르기·속도/ARM·Spring/앱 연결은 후속이다.
자체 점검이며 타인 리뷰·사용자 학습·병합·운영 배포 완료를 뜻하지 않는다.
