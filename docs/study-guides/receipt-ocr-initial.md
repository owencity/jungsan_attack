# 영수증 Python 초기 구현 — 변경 코드 기반 학습 위치

정책 기준: origin/main `3eb5254`의 STUDY_POLICY·STUDY_CATALOG.
이번 diff는 Python이다. Java/Kotlin 책·강의 목차를 Python 문법에 임의 대응하지 않는다.
최종 PR base/head는 PR 본문에서 고정하며, 자체 점검 기록은 receipt-ocr/VALIDATION.md다.
타인의 리뷰를 확보했다는 기록은 아니다.

## 기술별 학습 포인트

- `receipt-ocr/receipt_ocr/dictionary.py` `load_dictionary`: MD 표 파싱·Unicode 정규화·상충 별칭 기동 거절이 분류 정책에 미치는 영향.
- `receipt-ocr/receipt_ocr/extract.py` `group_rows`·`extract`: OCR 출력 순서와 이미지 좌표·표 열 순서의 차이가 수량/단가 추출에 미치는 영향.
- `receipt-ocr/receipt_ocr/model.py` `Token`·`Receipt`: 인식 신뢰도와 확인 후보 데이터의 검증·JSON 경계.
- `receipt-ocr/receipt_ocr/images.py` `decode_image`: 압축 파일 크기와 디코딩 픽셀 수의 차이, EXIF 방향과 메타데이터 제거.
- `receipt-ocr/receipt_ocr/api.py` `create_app`: lifespan 모델 초기화·내부 서비스 인증·스트리밍 제한·threadpool·추론 슬롯의 관계.
- `receipt-ocr/receipt_ocr/ocr.py` `PaddleEngine`: 한국어 인식 모델 선택과 엔진 출력→공통 토큰 변환 경계.
- `receipt-ocr/receipt_ocr/evaluate.py` `evaluate`: 실패 표본을 포함한 완전일치율·중복 품목 비교와 단건 지연의 해석.
- `receipt-ocr/tests/test_api.py` `test_concurrent_request_rejected_without_unbounded_queue`: 실제 겹친 요청과 처리 중 admission 제어를 Event로 재현하는 관점.
