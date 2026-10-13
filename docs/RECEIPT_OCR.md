# 영수증 추출 — Python 내부 API v2

> 2026-10-14 · 초기 구현. 사용자 승인: PaddleOCR 한국어 CPU + Python 추출 + FastAPI.
> Spring 공개 API·앱 연결·운영 배포·실제 영수증 정확도 검증은 후속이다.
> v2는 사용자 사전 팩트체크를 반영한다. v1의 브랜드별 기본 용기를 제거하고 명시된 용기만 읽는다.

## 경계

앱 → Spring(사용자 인증·총무 권한·OPEN 정산 단위) → Python → 확인용 입력 → 총무 확인 → 기존 차수 저장.
Python은 사용자·Gathering·SettlementUnit·DB를 모르며 정산·송금·저장을 수행하지 않는다.
기존 Java/Kotlin 기능을 이 Python에 중복 구현하지 않는다. 최초 코드는 독립 폴더이며 별도 저장소로 이동 가능하다.
사진·원문을 저장하지 않는 최소 구현으로 시작한다. 장기 보관·학습 활용을 승인한 것은 아니다.

## 요청과 응답

Python만 제공하는 `POST /api/v1/receipts/extract`. **현재 Spring에는 이 URL이 없다.**
Content-Type image/jpeg 또는 image/png의 바이너리 본문. multipart·URL·PDF는 받지 않는다.
내부 `X-Receipt-Service-Key`가 필요하다. 공개 로그인·쿠키/Bearer 정책을 대체하는 헤더가 아니다.
32자 이상 ASCII 비밀값 `RECEIPT_SERVICE_KEY`는 필수, 없음/짧은 값은 기동 실패.

```json
{
  "schemaVersion": 2,
  "total": 50000,
  "drinks": [
    {
      "name": "참이슬 (세부형 미상)", "category": "SOJU", "unit": "UNKNOWN",
      "unitPrice": 5000, "quantity": 3, "lineAmount": 15000,
      "servingType": "UNKNOWN", "volumeMl": null, "bottleCount": null,
      "confidence": 0.98, "reviewReasons": ["UNKNOWN_CONTAINER"]
    }
  ],
  "warnings": ["ITEMS_NEED_REVIEW"],
  "requiresConfirmation": true
}
```

- total/unitPrice/lineAmount: 정수 원 또는 null. quantity: 정수 또는 null.
- 추출 범위: 금액 0~10억 원(품목 단가·금액은 양수), 수량 1~1000. 운영 도메인 제한은 Spring/Core가 별도로 검증한다.
- category: SOJU / BEER / MAKGEOLLI / CHEONGJU_YAKJU / WINE / WHISKY_SPIRIT / COCKTAIL_MIXED / OTHER_ALCOHOL / UNKNOWN.
  내부 입력 후보 분류이며 법정 주종 확정값이 아니다. NON_ALCOHOL·NON_BEVERAGE 사전 행은 반환하지 않는다.
- unit: BOTTLE / CAN / GLASS / PITCHER / PACK / UNKNOWN. 브랜드만으로 병을 가정하지 않는다.
  기존 DrinkItem에는 unit이 없으므로 병 이외·미상은 총무가 수동 확인·매핑한 뒤 저장한다.
- servingType: DRAFT / UNKNOWN. 일반 생맥 메뉴에만 DRAFT, 생막걸리·브랜드의 생드래프트 이름으로 추측하지 않는다.
- volumeMl: 명시된 용량의 정수 mL 또는 null. 1~100,000mL, 정수 mL로 표현 못하는 소수는 null·확인 사유.
  L은 정확히 1000배 변환한다. 단위 없는 500·도수·제품명의 25·1000억은 용량/수량으로 추측하지 않는다.
- bottleCount: 병 용기와 양수 수량이 명시되고 관련 충돌이 없을 때 수량. 명확한 잔·캔·피처·생맥은 0,
  용기·수량 미상, 세트·팩·취소, 낮은 신뢰도·수량/금액 충돌은 null이다. UNKNOWN 주종은 null이다.
  항목의 null을 0으로 간주해 전체 병 수 합계를 확정하지 않는다. 모든 필드는 확인용 후보다.
- confidence: 품목 행의 가장 낮은 OCR 점수. 필드가 맞을 확률을 보증하는 값은 아니다.
- 항상 사람이 확인한다. null을 0으로 바꾸거나 경고 없는 후보를 자동 저장·자동 정산하지 않는다.
- 동일 이름이 여러 구매 행이면 각각 반환한다. 병 수만 합치면 단가 차이가 지워질 수 있다.
- 원본 OCR 텍스트·카드번호·승인번호·계좌·이미지 메타데이터는 응답하지 않는다.

## 확인 사유

| 필드 | 값 |
|---|---|
| drinks[].reviewReasons | COLUMN_HEADER_MISSING, ITEM_VALUES_MISSING, ITEM_OUT_OF_RANGE, ITEM_AMOUNT_MISMATCH, LOW_CONFIDENCE, UNIT_CONFLICT, UNIT_REQUIRES_MAPPING, UNKNOWN_CONTAINER, UNKNOWN_CATEGORY, QUANTITY_INVALID_OR_CONFLICT, VOLUME_INVALID_OR_CONFLICT, COMBO_OR_PROMOTION, RETURN_OR_CANCEL |
| warnings | OCR_NO_TEXT, TOTAL_NEEDS_REVIEW, TOTAL_NOT_FOUND, TOTAL_CONFLICT, NO_DRINK_ITEMS, ITEMS_NEED_REVIEW, DRINK_AMOUNT_EXCEEDS_TOTAL |

총액 후보가 충돌하면 null이다. 품목 금액 불일치는 읽은 후보와 사유를 함께 반환한다.
음식값이 포함된 총액과 술값은 같을 필요가 없다. 술값 초과만 모순으로 표시한다.
미등록 주류·OCR에서 아예 놓친 품목은 검출 보장을 하지 못하므로 사람 확인은 항상 필요하다.

## 사전·숫자 처리

- 실행 사전은 `receipt-ocr/data/drinks.md`의 4열 또는 5열 표다. 정책·공식 출처는
  `receipt-ocr/docs/receipt_alcohol_ocr_dictionary.md`에 분리했다. 현재 147개 정규화 표기다.
- 단위는 표기 자체에 근거가 있어야 한다. `테라 | 병`처럼 브랜드의 대표 포장을 지정한 사전은 실행을 거절한다.
- 완전 일치 상품명에서 명시 단위·용량·수량 표현만 분리한다. 알 수 없는 접미어를 버려 짧은 브랜드에 매칭하지 않는다.
  카스 0.0·진로 토닉워터·테라 제로는 긴 전체 상품명을 보호하고 일반 주류로 반환하지 않는다.
- 단가·수량·금액은 신뢰 가능한 3열 헤더와 세 숫자가 있어야 읽는다. 헤더 없이도 `2병`·`수량 2`·`x2`는 읽되 가격은 채우지 않는다.
- 음수·부호·소수 수량·잘못된 쉼표는 전체 숫자로 거절한다. `1.5병`을 5병, `-2병`을 2병으로 부분 매칭하지 않는다.
- 팩 구성 수와 판매 수량을 구분한다. 명시된 팩 판매 수량은 보존하지만 낱병/캔 수로 환산하지 않는다.
- 다중 줄 품목·좌표에 따른 완전한 표 인식·상호/광고 영역의 문서 분류는 아직 실제 샘플 개선 대상이다.

## 오류

이것은 내부 FastAPI 응답으로 `{"detail":{"code":"..."}}`다. Spring의 공개 오류 봉투로
변환하는 코드는 아직 없다. 새 공개 API를 연결할 때 같은 PR에서 별도 계약·테스트를 만든다.

| HTTP | code | 의미 |
|---|---|---|
| 401 | SERVICE_AUTH_REQUIRED | 내부 키 없음/다름 |
| 415 | IMAGE_FORMAT_UNSUPPORTED | 요청 미디어 타입이 JPEG/PNG가 아님 |
| 413 | IMAGE_TOO_LARGE | 요청 스트림 8MiB 초과 |
| 400 | IMAGE_SIZE_INVALID | 빈 파일 또는 디코더 크기 제한 |
| 400 | IMAGE_INVALID | 디코딩 실패·깨진 사진 |
| 400 | IMAGE_FORMAT_UNSUPPORTED | 실제 파일이 지원하지 않는 형식/다중 프레임 |
| 400 | IMAGE_DIMENSIONS_TOO_LARGE | 1,600만 픽셀 초과 |
| 503 | OCR_BUSY | 한 장 처리 중, Retry-After: 2 |
| 503 | OCR_UNAVAILABLE | 추론 실패, 내부 오류 메시지 미노출 |

## 가정·후속

- 별도 저장소 경로가 지정되지 않아 브랜치의 receipt-ocr 폴더에 모았다. 독립 저장소로 이동할 때 이 폴더를 그대로 옮긴다.
- 실제 샘플 평가 전이므로 최종 엔진 채택·ARM 배포는 확정하지 않는다. Redis/Kafka/k3s를 발동하지 않는다.
- 공개 Spring 업로드 API, 권한·시간 제한·요청 제한·통합 테스트, 앱 촬영 화면 연결은 후속이다.
- 8MiB·픽셀·한 장 동시 처리·신뢰도 0.85는 초기 자원/확인 정책이며 샘플 실측으로 조정한다.
- 기준 main 3eb5254. 읽은 열림 FC: 005·009·010·011·014·015·020·021. 이번 Python 추출은 해당 FC를 완료하지 않으므로 상태를 유지한다.
- FC-015의 단위별 권한과 FC-020의 자동 정산은 Spring 연결 때 지킨다. 영수증 추출만으로 저장/확정을 유발하지 않는다.
