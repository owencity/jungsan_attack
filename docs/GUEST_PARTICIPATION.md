# 비회원 참여와 본인 응답 API

FC-028의 백엔드 참여·응답 계약이다. 개인 금액과 보냈어요·총무 표 데이터는
[SETTLEMENT_VIEWS](SETTLEMENT_VIEWS.md)에서 추가한다. 총무 앱·회원 로그인은 유지한다.
음료 별도 분배, 비회원 계좌·메시지·신고/차단·스푼은 후속이다.
근거는 [ADR-023](ADR/023-guest-response-cookie.md)이다.

## 브라우저 자격

`POST /api/v1/guest/session` → **204**. 기존 frontend-origin과 정확히 같은 Origin을 요구한다.
처음에만 host-only `jeongsan_guest` JWT 쿠키를 설정한다. httpOnly·SameSite=Lax·운영 Secure·
Path=/api/v1/guest·고정 30일(Max-Age=2592000)이다. 유효 쿠키 재시도는 쿠키를 회전하지 않는다.
무효/만료 쿠키는 401 UNAUTHENTICATED, 다른/누락 Origin은 403 GUEST_ORIGIN_NOT_ALLOWED다.

웹은 초기화를 직렬화하고 완료 후 credentials: include로 아래 API를 호출한다. 토큰은
JSON·URL·localStorage·Authorization 헤더로 주고받지 않는다. 초기화만으로 DB 행을 만들지 않는다.
쿠키 삭제·브라우저 변경·만료 후 신원 복구와 회원 연결은 후속이다. 이름으로 복구하지 않는다.
다른 브라우저의 같은 사람을 자동 식별하는 보장은 없다. 기존 회원 쿠키는 유지한다.

## 참여와 최초 응답

`POST /api/v1/guest/join/{shareToken}` → **200**. 비회원 쿠키·정확한 Origin·JSON이 필요하다.
회원용 POST /api/v1/join/{shareToken}은 변경하지 않는다.

```json
{"displayName":"김하나","settlementUnitId":10,"responses":[{"roundId":101,"type":"SOBER"}]}
```

이름은 앞뒤 공백 제거 후 2~10 Unicode 코드포인트, 중간 제어문자 금지다. 실명 인증은 아니다.
responses는 생략 가능하며 최대 1000개다. 기존 ABSENT/SOBER/DRANK만 받으며 차수 중복·다른 단위 차수·
본인 EXEMPT는 거절한다. 음료 O/X 필드는 없다.

브라우저·술자리가 신원 키이고 브라우저·술자리·단위가 참여의 멱등 키다. 다른 브라우저의 동명이인은
별도 신원이다. 이미 포함된 단위의 POST는 새 responses로 덮지 않고 현재 상태를 반환한다.
이름이 다르면 409 GUEST_NAME_MISMATCH다. 자동 정산 직후 같은 POST 재시도도 200이다.
신규 단위 참여는 OPEN일 때만 가능하고, 총무가 제외한 단위에는 재가입할 수 없다.

참여자·권한·명단·응답·revision·총무 알림·자동 정산은 단위→술자리 잠금 아래 한 트랜잭션이다.
응답 검증 실패는 참여까지 롤백한다. 회원 ID 0번을 만들거나 알림 수신자로 저장하지 않는다.
응답은 다음 본인 조회와 같다.

## 본인 재조회

`GET /api/v1/guest/gatherings/{gatheringId}/me` → **200**. 비회원 쿠키를 요구한다.

```json
{
  "gatheringId":1,"title":"10/31 술자리","participantId":2,"displayName":"김하나",
  "settlementUnits":[{"id":10,"status":"OPEN","inputRevision":3,"included":true}],
  "rounds":[{"id":101,"settlementUnitId":10,"seq":1,"total":10000}],
  "responses":[{"roundId":101,"type":"SOBER","source":"SELF"}]
}
```

단위는 본인이 속했거나 제외된 단위만 ID순이다. included=false 단위의 차수·응답은 주지 않는다.
차수와 본인 응답은 차수 순서다. 미응답은 행 부재로 ABSENT와 구분한다.
전체 명단·타인 응답·대화·송금·계좌·shareToken·인증 값·예상/확정 부담액은 없다.
참여하지 않은 술자리, 삭제된 참여자, 표식 없는 탈퇴 좌석은 403 NOT_PARTICIPANT다.
성공한 비회원 응답은 Cache-Control: no-store다.

## 본인 응답 수정

`PUT /api/v1/guest/gatherings/{gatheringId}/settlement-units/{settlementUnitId}/responses/me` → **204**.
비회원 쿠키·정확한 Origin·JSON을 요구한다.

```json
{"answers":[{"roundId":101,"type":"DRANK"}]}
```

participantId를 받지 않고 브라우저 표식으로 본인만 결정한다. ACTIVE 명단의 OPEN 단위만 수정한다.
총무의 EXEMPT는 SELF로 덮지 않는다. 전송한 답변만 변경하고 빠진 차수는 삭제하지 않는다.
같은 type/source 재시도는 revision·알림을 늘리지 않는다. 마지막 응답은 기존 자동 정산을 실행한다.
정산 뒤 수정은 409 SETTLEMENT_UNIT_NOT_OPEN, 제외된 단위는 403 NOT_SETTLEMENT_UNIT_MEMBER,
다른 단위 차수는 404 ROUND_NOT_FOUND다.

## 총무와 기존 데이터

총무는 기존 API로 비회원 대리 응답·명단 추가/제외·다른 단위 명단 재사용을 할 수 있다.
비회원 결제자 지정은 409 PAYER_LOGIN_REQUIRED다. 계좌·수취 권한이 아직 없기 때문이다.
비회원은 기존 회원 관리·계좌·송금·메시지 API에 접근할 수 없다. 본인 송금의 보냈어요는 별도 guest API다.

021-guest-participations.yaml의 changeSet 031을 추가한다. nullable participants.user_id는 유지하고
별도 표식으로 비회원과 탈퇴 좌석을 구분한다. 참여자 삭제 시 표식은 FK CASCADE로 삭제된다.
Core 입력은 기존 participantId·이름·응답이므로 계산 규칙은 바꾸지 않는다.
