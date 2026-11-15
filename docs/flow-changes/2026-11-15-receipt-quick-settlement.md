# FC-038 간단 N빵과 영수증 촬영 입력

- **상태:** 열림 · 구현·검증 중. 운영 제공 완료가 아니다.
- **요청:** 2026-11-15 CTO가 간단 N빵과 영수증 OCR을 이번 배포에 함께 넣도록 승인했다.
- **바뀐 흐름:** 총무가 간단 N빵 또는 기존 상세 정산을 선택한다. 촬영하거나 최대 5장을 선택하고 한 장씩 인식한 금액을 확인·수정한 뒤 적용한다.
- **백엔드 영향:** 로그인 전용 후보 추출·간단 계산 API. DB와 기존 확정 정산 변경 없음.
- **기존 문서와 관계:** FC-029의 총무 직접 확정, FC-037의 별도 총인원을 유지한다. OCR 도입만으로 ADR-016~018의 Redis·Kafka·k3s를 도입하지 않는다.

## 화면 흐름

간단 N빵은 로그인한 총무가 총금액과 본인 포함 인원(2~50명)을 입력하고 결과 이미지를 저장한다. 초대·참석 응답·명단을 만들지 않는다. 영수증은 선택 사항이며 여러 장의 확인된 총액을 합산한다. 술값은 따로 나누지 않는다.

결과는 다른 사람 한 명이 보낼 금액, 결제자 본인 부담, 받을 합계를 구분한다. 기존 core처럼 비결제자 송금액을 1원 올림하고 차액은 결제자 몫에서 조정한다. 10,000원·3명은 두 명이 각 3,334원을 보내고 결제자는 3,332원을 부담한다. 합계는 10,000원이다. 계산 결과를 저장하거나 실제 송금하지 않는다.

상세 정산은 차수 편집에서 촬영·선택하거나 술자리에서 여러 사진을 넣고 사진마다 OPEN 차수를 지정한다. 적용 전 차수를 바꿀 수 있고, 새 차수는 기존 추가 화면에서 만든다. 총액·술 이름·단가·수량을 원본과 대조한다. 같은 차수의 확인된 후보끼리 합산한다.

기존값이 있으면 기존 금액과 후보를 보여주고 **교체 또는 추가**를 명시적으로 선택한다. 이는 기존값을 조용히 덮지 않기 위한 구현 가정이다. 같은 사진·결과의 중복 적용을 막고 실패한 사진만 재시도·제외한다. 취소·오류는 기존값을 보존한다. 여러 차수 저장은 차수별 기존 API를 사용하므로 성공한 차수와 다시 저장할 차수를 구분한다.

사진과 후보는 임시 화면 상태다. 이번에는 서버에 영수증 보관함을 만들지 않는다. 저장 후에는 기존 차수 편집으로 금액을 수정한다.

## API 계약

기존 웹 httpOnly 쿠키 또는 앱 전용 Bearer JWT 인증을 사용한다. 삭제된 계정은 거절한다.

### 영수증 한 장 추출

```
POST /api/v1/receipts/extract
POST /api/v1/gatherings/{gid}/settlement-units/{sid}/receipts/extract
Content-Type: image/jpeg 또는 image/png
본문: 이미지 바이너리, 최대 8MiB (multipart/base64 아님)
```

단위 경로는 활성 총무·OPEN 상태를 추출 전후 확인한다. 간단 경로는 계정 존재를 전후 확인한다. 추론 동안 DB 트랜잭션·행 잠금을 유지하지 않는다. 성공 200과 `Cache-Control: no-store`를 반환한다.

```json
{"schemaVersion":2,"total":50000,"drinks":[{"name":"참이슬 (세부형 미상)","category":"SOJU","unit":"UNKNOWN","unitPrice":5000,"quantity":3,"lineAmount":15000,"servingType":"UNKNOWN","volumeMl":null,"bottleCount":null,"confidence":0.98,"reviewReasons":["UNKNOWN_CONTAINER"]}],"warnings":["ITEMS_NEED_REVIEW"],"requiresConfirmation":true}
```

확실하지 않은 숫자는 null이며 인식하지 못한 술은 빠질 수 있다. 경고가 없어도 원본 대조와 확인이 필요하다. 간단 정산은 total만, 상세 정산은 검토한 술 항목도 사용한다.

| 상태 | 코드·의미 |
|---|---|
| 400 | IMAGE_INVALID / IMAGE_SIZE_INVALID / IMAGE_DIMENSIONS_TOO_LARGE |
| 401 | 기존 UNAUTHENTICATED. 내부 서비스 키 오류는 사용자 로그인 오류로 내보내지 않음 |
| 403/404/409 | 기존 단위 권한·없음·OPEN 아님 계약 |
| 413/415 | IMAGE_TOO_LARGE / IMAGE_FORMAT_UNSUPPORTED |
| 429 | OCR_REQUEST_LIMIT: 사용자당 60초 10회, Retry-After 초 |
| 502 | OCR_BAD_RESPONSE: 내부 응답 형식 검증 실패 |
| 503 | OCR_BUSY(동시 처리 중, Retry-After 2초) / OCR_UNAVAILABLE |
| 504 | OCR_TIMEOUT: 내부 요청 시간 초과 |

두 경로의 제한을 함께 센다. 서버는 동시 1건이며 대기열·자동 재시도를 두지 않는다. 앱도 순차 요청하고 실패를 성공으로 처리하지 않는다. Python 서비스와 키는 외부에 공개하지 않는다.

### 간단 N빵

```
POST /api/v1/settlements/quick
Content-Type: application/json
{"total":10000,"headcount":3}
```

성공 200:

```json
{"total":10000,"headcount":3,"participantAmount":3334,"payerAmount":3332,"incomingTotal":6668}
```

total은 정수 1~1천만 원, headcount는 결제자 포함 2~50명이다. 입력 검증 400, 이 신규 API의 core 계산 거절은 422 VALIDATION_FAILED다. 금액이 너무 작아 조정자 부담이 음수면 errors[].code=NEGATIVE_ADJUSTED_AMOUNT로 거절한다. 기존 방 확정 API의 상태 코드는 바꾸지 않는다. Spring은 메모리의 계산 입력만 core에 넘기고 가짜 사용자·술자리·참여자를 DB에 만들지 않는다.

## 운영·검증

Python은 OCI 내부 전용 네트워크의 별도 컨테이너에서 실행한다. 모델을 빌드 시 포함하고 재기동 중 다운로드하지 않는다. 초기 제한은 메모리 3GiB·CPU 1개 상당이다. 실제 ARM 추론과 기존 서비스 지연을 측정해야 한다. 사진·OCR 원문·키·사용자 정보를 로그나 지표 태그에 남기지 않는다.

기존 32장 평가에서 조건 일치는 14장이었다. 따라서 완전 자동 정산이나 정확도를 보장하지 않으며 수동 검토를 필수로 유지한다. 최신 서버·앱·ARM 실행 결과와 운영 반영은 별도 배포 기록에 추가한다.

- **반영할 곳:** API v27, ADR-024, 앱 RECEIPT_INPUT, 배포 기록.
- **백엔드 반영·근거:** 구현 중. 최신 검증·배포 완료 후 갱신.
