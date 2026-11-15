# Spring 연결 인계 · 2026-10-19

현재 구현은 Python 내부 API까지다. 이 문서와 예제는 Spring 구현·운영 연결 완료를 뜻하지 않는다.
내부 JSON은 schemaVersion 2이며 [전체 계약](../../docs/RECEIPT_OCR.md)이 기준이다.

## 호출

Spring이 `POST http://127.0.0.1:8091/api/v1/receipts/extract`에 JPEG/PNG 바이트를 보낸다.
Content-Type은 `image/jpeg` 또는 `image/png`, 인증은 `X-Receipt-Service-Key`다.
JSON, multipart, 이미지 URL은 받지 않는다. 같은 컨테이너가 아니면 127.0.0.1 대신 사설 서비스 주소를 쓴다.
Python 공인 포트를 열지 않고 키는 서버 환경에서만 읽는다. 앱에 내부 키·주소를 전달하지 않는다.

인식에 성공한 HTTP 200에도 total·quantity·unitPrice·lineAmount가 null일 수 있다.
`requiresConfirmation`은 항상 true다. warnings가 비어도 누락이 없다는 뜻은 아니다.
정상 반환·필드 누락 예제는 [response.review-required.json](../examples/response.review-required.json)을 사용한다.
미상 단가를 총액이나 항목 금액에서 역산하거나 null을 0으로 바꾸지 않는다.
병이 명시되지 않은 브랜드에는 bottleCount=null이며, quantity를 병 수로 바로 저장하지 않는다.

## Spring 쪽 구현 범위

1. 기존 `@LoginUser` 인증 후 Gathering/SettlementUnit 관계, 해당 단위 총무, OPEN 상태를 검사한다.
   FC-015의 다른 단위 총무에게 업로드 권한을 열지 않는다. 새 DTO/공개 경로는 API.md와 함께 확정한다.
2. 스트림을 제한해서 최대 8MiB만 읽고 Python에 전달한다. 업로드 전에 읽은 바이트 수로도 제한한다.
   Python은 추가로 실제 JPEG/PNG, 1,600만 픽셀, 긴 변 2400px·메타데이터 제거를 처리한다.
3. 연결/응답 시간 제한과 사용자별 요청 제한을 명시한다. 대기 없는 Python 한 장 처리 정책을 고려해 자동 재시도하지 않는다.
   CPU 추론은 HTTP 연결 종료만으로 멈추지 않는다. DB 트랜잭션·행 잠금을 유지한 채 외부 추론을 기다리지 않는다.
4. 내부 오류를 공개 API의 기존 오류 봉투로 변환한다. 원본 오류, 키, 사진, OCR 원문을 로그에 남기지 않는다.
5. 후보만 반환하고 저장하지 않는다. 총무가 확인한 값은 기존 차수 저장 API에 보내며 기존 검증·권한을 다시 적용한다.
   FC-020의 자동 정산 조건은 기존 저장/응답 흐름이 처리한다. 추출 요청으로 revision·명단·정산·송금을 변경하지 않는다.

## 오류 매핑 시 보존할 의미

| Python 응답 | Spring에서 구분할 상황 |
|---|---|
| 401 SERVICE_AUTH_REQUIRED | 내부 연결 설정 오류. 앱 사용자에게 재로그인을 요구하는 401로 그대로 넘기지 않는다 |
| 400 IMAGE_* | 사진 디코딩/픽셀/실제 형식 오류. 지원되는 사진 재선택 안내 |
| 413 IMAGE_TOO_LARGE | 업로드 크기 초과 |
| 415 IMAGE_FORMAT_UNSUPPORTED | Content-Type 미지원 |
| 503 OCR_BUSY, Retry-After: 2 | 처리 중. 재시도 가능 시점을 전달하고 내부 재시도 폭주를 만들지 않는다 |
| 503 OCR_UNAVAILABLE / 연결 실패 / 시간 초과 | 일시적 인식 장애. 내부 예외를 숨기고 수동 입력 가능하게 안내 |

공개 오류 코드·상태는 아직 구현하지 않았다. 위 표는 의미와 금지 사항이며 새 코드를 이미 제공한다는 뜻이 아니다.

## 연결 완료 판정

- 실제 Spring→Python 요청에서 총액·수량·null·확인 사유가 그대로 앱에 도달해야 한다.
- 미로그인·다른 단위 총무·정산된 단위는 Python을 호출하지 않아야 한다.
- 크기 초과·잘못된 사진·혼잡·시간 초과·키 불일치가 공개 봉투로 변환되고 다음 요청이 처리돼야 한다.
- 기존 인증 가드 테스트와 성공/실패 HTTP 테스트를 통과해야 한다.
- 대표 실제 사진 정확도와 OCI ARM 설치·메모리·처리시간 검증은 별도다.

REQUIREMENTS §9는 앱 전용이며 웹은 수동 입력이다. 대화에서 설명한 왕복 구조만으로 웹 OCR 기능을 추가하지 않는다.
