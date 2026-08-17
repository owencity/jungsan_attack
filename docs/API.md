# 정산어택 — API 계약 · v1

이 문서는 **프론트와 백엔드가 코드를 짜기 전에 합의하는 형식**이다.

계약이 없으면 필드 이름 하나(`alcohol` vs `alcoholAmount`)가 어긋나도 양쪽을 다시 쓴다.
그리고 `core/Validation.kt` 가 이미 갖고 있는 18개 오류 코드가 화면의 어느 문구로
이어지는지 아무 데도 정해져 있지 않다. 그 두 가지를 여기서 못 박는다.

**읽기 응답의 형태는 `src/jeongsan/types.ts` 가 이미 확정한 것이다.** 이 문서가 새로
정하는 것은 **쓰기와 인증, 그리고 오류**다.

---

## 0. 전제

| 항목 | 값 | 이유 |
|---|---|---|
| Base path | `/api/v1` | |
| 금액 | `Long` · 원 단위 · 소수 없음 | 계산 엔진이 정수만 다룬다 (`ADR-001`) |
| id | JSON `number` | ↓ §0.1 |
| 시각 | ISO-8601 + offset (`2026-08-17T21:40:00+09:00`) | 컬럼이 `TIMESTAMPTZ` |
| 날짜 | `YYYY-MM-DD` (모임 날짜) | 시각이 아니다. UTC 변환 금지 — ↓ §0.2 |
| 인증 | `Authorization: Bearer <jwt>` | ↓ §2 |
| 문자셋 | UTF-8 | |

### 0.1 id 를 number 로 보내는 이유

DB는 `BIGINT`, Kotlin은 `Long`, JavaScript는 `number`(배정도 부동소수) 다.
**JS의 안전 정수 상한은 2^53−1 = 9,007,199,254,740,991 이다.** 그보다 큰 `Long` 을
JSON 숫자로 보내면 프론트에서 조용히 값이 바뀐다.

시퀀스를 1부터 쓰므로 **9,007조 건**을 넘길 일이 없다. 문자열로 감싸는 비용
(모든 비교·Map 키에서 변환)이 얻는 것보다 크다. **그래서 number 로 보낸다.**

> ⚠ 이 결정은 **id를 Snowflake·UUIDv7 정수 같은 것으로 바꾸는 순간 깨진다.**
> 그때는 문자열로 바꿔야 한다. id 생성 방식을 바꾸려면 이 절을 먼저 볼 것.

### 0.2 모임 날짜에 시각을 붙이지 않는 이유

`gatheringDate` 는 **"8월 14일 회식"** 이라는 사람의 표현이고 순간이 아니다.
`Date` 로 다루면 KST 오전 9시 이전에 만든 모임이 UTC로 밀려 **전날**이 된다.

프론트는 `DatePicker.toISO()` 가 로컬 자정 기준으로 만든 `YYYY-MM-DD` 문자열을 보낸다.
백엔드도 `LocalDate` 로 받는다. **어느 쪽도 `toISOString()`/`Instant` 로 변환하지 않는다.**

---

## 1. 공통 형식

### 1.1 성공

**봉투를 씌우지 않는다.** 본문이 곧 자원이다.

```
GET /api/v1/gatherings/1
200
{ "id": 1, "name": "8월 팀 회식", ... }
```

목록도 배열 그대로 돌려준다.

```
GET /api/v1/gatherings
200
[ { "id": 1, ... }, { "id": 2, ... } ]
```

> `api.ts` 의 `json.data ?? json` 은 어느 쪽인지 정하지 않아서 둘 다 받게 해둔 것이다.
> 이 계약이 정해지면 **`data` 분기를 지운다.**

### 1.2 오류 — 언제나 같은 모양

```json
{
  "code": "GATHERING_CONFIRMED",
  "message": "이미 정산이 확정되었습니다.",
  "errors": null
}
```

**검증 실패만 `errors` 배열이 채워진다.** `Validator.validate()` 가
`List<ValidationError>` 를 돌려주기 때문이다 — 차수를 5개 입력한 주최자에게
**첫 번째 오류만 알려주면 5번 왕복한다.**

```json
{
  "code": "VALIDATION_FAILED",
  "message": "입력을 확인해주세요.",
  "errors": [
    {
      "code": "ALCOHOL_EXCEEDS_TOTAL",
      "message": "2차 · 호프집의 술값(70,000원)이 총액(62,000원)보다 큽니다.",
      "roundId": 2, "extraId": null, "participantId": null
    },
    {
      "code": "PAYER_NOT_FOUND",
      "message": "3차 · 노래방의 결제자가 참여자 목록에 없습니다.",
      "roundId": 7, "extraId": null, "participantId": 91
    }
  ]
}
```

**`message` 는 서버가 만든 사용자용 문구를 그대로 쓴다.** `Validation.kt` 가 이미
`"${round.label}의 술값(${round.alcohol}원)이 총액(${round.total}원)보다 큽니다."`
처럼 어느 차수 무엇이 문제인지 담아 만든다. 프론트가 코드로 문구를 다시 만들면
같은 말을 두 곳에서 관리하게 된다.

**`roundId`·`extraId`·`participantId` 는 프론트가 해당 입력란을 붉게 만드는 데 쓴다.**
문구만 있으면 사용자는 5개 차수 중 어디를 고쳐야 하는지 스크롤로 찾는다.

### 1.3 HTTP 상태코드

| 코드 | 언제 | `code` 예 |
|---|---|---|
| `200` | 조회·수정 성공 | |
| `201` | 생성 성공 | |
| `204` | 삭제 성공 (본문 없음) | |
| `400` | 계산 엔진 검증 실패 · 요청 형식 오류 | `VALIDATION_FAILED` `MALFORMED_REQUEST` |
| `401` | 토큰 없음·만료·위조 | `UNAUTHENTICATED` `TOKEN_EXPIRED` |
| `403` | 인증은 됐지만 권한 없음 | `NOT_HOST` `NOT_SELF` |
| `404` | 없거나, **있어도 알려주면 안 되는** 것 | `NOT_FOUND` `INVALID_SHARE_TOKEN` |
| `409` | 상태가 맞지 않음 | `GATHERING_CONFIRMED` `STALE_INPUT_HASH` |
| `429` | 너무 많이 요청 | `TOO_MANY_REQUESTS` |

**`403` 과 `404` 를 구분하는 기준** — 남의 모임을 열어보려는 요청에 `403` 을 주면
*"그 모임은 존재한다"* 를 알려주는 것이다. **자기 것이 아니면 `404` 로 답한다.**
`403` 은 자기 모임인데 역할이 안 맞을 때만 쓴다(참여자가 주최자 기능 호출).

### 1.4 오류 코드 — 전체 목록

**계산 엔진에서 오는 것 (18개)** — `core/Validation.kt` 의 `ErrorCode` 를 그대로 쓴다.
문자열이 하나라도 어긋나면 프론트가 처리하지 못하므로 **직접 적지 말고 enum 이름을 쓴다.**

| 코드 | 시점 | 뜻 |
|---|---|---|
| `TOTAL_NOT_POSITIVE` | 저장 | 총액이 0 이하 |
| `ALCOHOL_NEGATIVE` | 저장 | 술값이 음수 |
| `ALCOHOL_EXCEEDS_TOTAL` | 저장 | 술값 > 총액 |
| `PAYER_NOT_FOUND` | 저장 | 결제자가 참여자에 없음 |
| `BEARER_NOT_FOUND` | 저장 | 부담자가 참여자에 없음 |
| `INVALID_DRINK_ITEM` | 저장 | 병 수·단가가 1 미만 |
| `INVALID_ROUNDING_UNIT` | 저장 | 10·100 이 아님 |
| `DRANK_WITHOUT_ATTEND` | 저장 | 불참인데 음주 |
| `DUPLICATE_ID` | 저장 | 참여자·차수·기타항목 id 중복 |
| `DUPLICATE_ROUND_SEQ` | 저장 | 차수 순번 중복 |
| `AMOUNT_TOO_LARGE` | 저장 | 1조 원 초과 (0을 더 찍은 것) |
| `TOO_FEW_PARTICIPANTS` | **확정** | 2명 미만 |
| `NO_ATTENDEE` | **확정** | 그 차수에 참석자 0명 |
| `NO_NON_EXEMPT_ATTENDEE` | **확정** | 참석자가 전원 면제 |
| `NO_DRINKER_WITH_ALCOHOL` | **확정** | 술값이 있는데 음주자 0명 |
| `NO_NON_EXEMPT_BEARER` | **확정** | 기타항목 부담자가 없음 |
| `ALL_EXEMPT` | **확정** | 전원 면제 |
| `NEGATIVE_FINAL_AMOUNT` | **확정** | 대표결제자 몫이 음수 |

**저장(SAVE)과 확정(CONFIRM)의 검증 범위가 다르다.** 저장 시점에 인원·배분까지
검증하면 **아직 아무도 체크하지 않아 참석자가 0명이므로 주최자가 차수를 입력조차
못 한다** (`CALC_RULES.md` §4). 그래서 쓰기 API 는 `SAVE`, 확정 API 는 `CONFIRM` 으로 검증한다.

**API 계층에서 나오는 것**

| 코드 | HTTP | 뜻 |
|---|---|---|
| `UNAUTHENTICATED` | 401 | 토큰 없음 |
| `TOKEN_EXPIRED` | 401 | 만료 — 프론트는 재로그인으로 보낸다 |
| `PROVIDER_AUTH_FAILED` | 401 | 카카오가 code 를 거부 |
| `NOT_HOST` | 403 | 주최자 전용 기능 |
| `NOT_SELF` | 403 | 남의 체크·입금 상태를 바꾸려 함 |
| `NOT_FOUND` | 404 | 없음 또는 내 것이 아님 |
| `INVALID_SHARE_TOKEN` | 404 | 링크가 틀리거나 재발급됨 |
| `GATHERING_CONFIRMED` | 409 | 확정 후 차단된 동작 |
| `GATHERING_COLLECTING` | 409 | 확정 전에는 못 하는 동작 |
| `STALE_INPUT_HASH` | 409 | 미리보기 이후 입력이 바뀜 (ADR-004) |
| `ALREADY_JOINED` | 409 | 이미 참여 중 |
| `ALREADY_SCHEDULED` | 409 | 이미 삭제 예정인 모임 |
| `PARTICIPANT_HAS_PAYMENT` | 409 | 입금 표시된 사람은 명단에서 못 뺀다 |
| `TOO_MANY_REQUESTS` | 429 | shareToken 무작위 대입 방어 |

---

## 2. 인증

### 2.1 로그인 — `POST /api/v1/auth/{provider}`

`{provider}` 는 `kakao` | `google`.

```
POST /api/v1/auth/kakao
{
  "code": "3xY...",
  "redirectUri": "https://jungsan.devkdk.com/auth/callback"
}
```

```
200
{
  "token": "eyJhbGciOi...",
  "expiresAt": "2026-08-31T21:40:00+09:00",
  "user": {
    "id": 12,
    "nickname": "🌸봄이🌸",
    "profileImage": "https://k.kakaocdn.net/...",
    "provider": "kakao",
    "tier": "FREE"
  },
  "firstLogin": true
}
```

**`redirectUri` 를 요청에 담는 이유** — 카카오는 토큰 교환 시 인증 때 쓴 것과
**같은 redirect_uri** 를 요구한다. 로컬(`localhost:5173`)·본배포
(`jungsan.devkdk.com`)·포트폴리오(`www.devkdk.com`) 세 곳이 다르므로
서버가 고정값을 쓰면 로컬에서 로그인이 안 된다.

> **서버는 이 값을 검증한다.** 등록된 화이트리스트에 없으면 `400 MALFORMED_REQUEST`.
> 검증 없이 그대로 카카오에 넘기면 공격자가 자기 서버로 code 를 받게 만들 수 있다.

**`code`·`클라이언트 시크릿`은 서버만 다룬다.** 프론트는 `code` 를 넘기는 것까지다.

**`firstLogin` 을 주는 이유** — 최초 로그인이면 화면이 안내를 더 보여줄 수 있다.
UPSERT 결과를 서버는 알지만 프론트는 알 수 없다.

### 2.2 내 정보 — `GET /api/v1/auth/me`

```
200   { "id": 12, "nickname": "🌸봄이🌸", ... }      // 2.1 의 user 와 같은 모양
401   TOKEN_EXPIRED
```

**새로고침할 때마다 프론트가 이걸 부른다.** localStorage 의 토큰이 아직 유효한지
서버만 알 수 있고, 만료된 토큰으로 화면을 그려두면 첫 조작에서 튕긴다.

### 2.3 토큰 정책

```
방식        JWT (HS256) · Authorization: Bearer
저장        localStorage
만료        14일
갱신        없다 — 만료되면 다시 로그인
담는 것     userId, provider, exp        ← 닉네임·프로필은 담지 않는다
```

**localStorage 를 쓰는 이유** — 쿠키를 쓰면 `jungsan.devkdk.com`(프론트)과
API 호스트가 다르므로 크로스 사이트 쿠키가 되고, `SameSite=None; Secure` +
`credentials: include` + CORS `allow-credentials` 가 전부 맞아야 한다.
**MVP에서 이 조합은 디버깅 비용이 크다.** 헤더로 보내면 CORS 가 단순해진다.

> 대가는 **XSS 에 노출된다**는 것이다. 다만 이 앱은 금액을 표시할 뿐
> **송금을 실행하지 않고**(SPEC §9), 담는 정보도 닉네임·프로필 사진뿐이다.
> 결제를 붙이는 날 이 결정을 다시 봐야 한다.

**갱신 토큰을 만들지 않는다 (확정).** refresh 토큰은 저장·회전·폐기 설계가 딸려온다.
**이 앱이 들고 있는 것은 닉네임·프로필 사진·모임 금액뿐이고 송금을 실행하지 않는다.**
그 정보량에 refresh 토큰 수명주기를 붙일 이유가 없다.

**1년에 몇 번 쓰는 앱**에서 14일마다 카카오 버튼을 한 번 더 누르는 비용이 그보다 작다.
카카오톡 인앱 브라우저에서는 이미 로그인돼 있어 탭 두 번이면 끝난다.

> 14일은 **한 모임의 생애를 덮는 길이**로 잡은 것이다. 술자리 다음날 정산하고,
> 입금 확인이 며칠에 걸쳐 끝난다. 그보다 짧으면 정산 도중에 튕기고,
> 갱신이 없으므로 길게 잡을수록 탈취된 토큰이 오래 살아 있다.

**닉네임을 토큰에 담지 않는 이유** — 사용자가 카카오에서 닉네임을 바꾸면
토큰 안의 값이 14일 동안 낡은 채로 남는다.

---

## 3. 모임 — 주최자

### 3.1 목록 — `GET /gatherings`  (H0)

```
200   GatheringSummary[]          // types.ts
```

`status` 로 정렬하지 않는다. **`date DESC` 다** — 사용자가 찾는 것은 최근 모임이다.

### 3.2 생성 — `POST /gatherings`  (H1)

```
POST /gatherings
{
  "name": "8월 팀 회식",
  "date": "2026-08-14",
  "hostName": "동규",
  "expectedCount": 5,
  "roundingUnit": 10,
  "payout": { "bankName": "국민은행", "accountNo": "123456-78-901234", "accountHolder": "김동규" }
}
```

```
201   Gathering                   // shareToken 포함. 프론트는 이걸로 링크를 만든다
400   VALIDATION_FAILED           // INVALID_ROUNDING_UNIT 등
```

| 필드 | 필수 | 비고 |
|---|---|---|
| `name` | ✔ | |
| `date` | ✔ | |
| `hostName` | ✔ | 주최자의 `Participant.name`. 닉네임이 기본값이지만 바꿀 수 있다 |
| `expectedCount` | ✔ | **정원 초과 승인 게이트의 기준.** 비면 방어가 통째로 사라진다 |
| `roundingUnit` | | 없으면 10 |
| `payout` | | 없어도 된다. 나중에 등록 가능 |

**서버가 같이 하는 일** — 주최자 자신의 `Participant` 를 만들고
`hostParticipantId` 에 연결한다. **주최자도 참여자다.** 안 그러면 주최자가
차수 결제자로 지정될 수 없다.

### 3.3 수정 — `PATCH /gatherings/{id}`

```
PATCH /gatherings/1
{ "expectedCount": 6 }              // 보낸 필드만 바꾼다
```

`name` `date` `expectedCount` `roundingUnit` `payout` 을 바꿀 수 있다.

```
200   Gathering
403   NOT_HOST
409   GATHERING_CONFIRMED           // roundingUnit 만 — 금액이 바뀌므로
```

**`expectedCount` 는 확정 후에도 바꿀 수 있다.** 계산에 쓰이지 않는 숫자다.
`roundingUnit` 은 금액을 바꾸므로 확정 상태에서 막고, 되돌린 뒤에 고치게 한다.

### 3.4 링크 재발급 — `POST /gatherings/{id}/share-token`

```
201   { "shareToken": "b7xk2mq" }
403   NOT_HOST
```

**옛 토큰은 즉시 죽는다.** 단톡방을 잘못 골라 뿌렸을 때 되돌릴 방법이 이것뿐이다
(SPEC §8). 이미 참여한 사람은 영향받지 않는다 — 참여 이후에는 토큰을 쓰지 않는다(§5).

### 3.5 삭제 — 두 트랙으로 나눈다

**바로 지우지 않는다.** 모임 하나에 다섯 사람의 금액과 입금 기록이 들어 있고,
잘못 지우면 되돌릴 방법이 없다. 그런데 삭제를 아예 막으면 시험용으로 만든
모임이 목록에 영구히 남는다.

```
[삭제] ──(1트랙: 화면 확인)──> [삭제 예정] ──(2트랙: 7일 경과)──> 실제 삭제
                                    │
                                    └──(되돌리기)──> 정상
```

**1트랙은 화면이 막는다** — 무엇이 사라지는지 숫자로 보여주고 확인을 받는다.
`"참여자 5명 · 입금 3건 · 확정된 정산이 함께 삭제됩니다"`.

**2트랙은 서버가 7일을 기다린다** — 실수를 알아챌 시간이다.

```
DELETE /gatherings/{id}
200   { "deleteScheduledAt": "2026-08-24T21:40:00+09:00" }
403   NOT_HOST
409   ALREADY_SCHEDULED
```

```
POST /gatherings/{id}/restore
200   Gathering                    // deleteScheduledAt = null
403   NOT_HOST
404   NOT_FOUND                    // 이미 실제 삭제됨
```

`Gathering` 에 필드가 하나 늘어난다.

```
deleteScheduledAt: string | null      // null 이면 정상. 값이 있으면 그 시각에 사라진다
```

#### 삭제 예정 기간에도 모임은 정상 동작한다

**목록에 배지가 붙고 삭제 예정일이 보이는 것 말고는 아무것도 달라지지 않는다.**
읽기도 쓰기도 참여자 링크도 그대로 산다.

이유는 참여자 쪽이다. **확정된 모임을 삭제 예정으로 걸어놓고 링크를 죽이면,
아직 입금하지 않은 사람이 계좌번호를 볼 수 없다.** 7일을 두는 목적은
*"주최자가 실수를 되돌릴 시간"* 이고, 그 기간에 참여자를 막는 것은
안전을 하나도 더하지 않으면서 정산을 멈춘다.

> **`DELETE` 가 `204` 가 아니라 `200` + 본문인 이유** — 지운 것이 아니라
> 예약한 것이다. 화면이 `"8월 24일에 삭제됩니다"` 를 그려야 하므로 날짜를 돌려준다.

#### 7일 뒤 실제 삭제

하루 한 번 도는 스케줄러가 `deleteScheduledAt <= now()` 인 모임을 **실제로 지운다**
(`Participant`·`Round`·`DrinkItem`·`ExtraItem`·`Attendance` CASCADE).

**행을 남겨두고 숨기지 않는다.** 참여자의 닉네임과 프로필 사진이 들어 있어,
영구 보관하면 지웠다고 알린 개인정보를 계속 들고 있는 것이 된다.

> 단일 VM 이므로(`ADR-006`) 스케줄러 인스턴스가 둘일 걱정이 없다.
> Spring `@Scheduled` 하나로 충분하고 별도 배치 인프라를 두지 않는다.

---

## 4. 차수와 기타 항목 — 편집폼이 부르는 것

경로를 모임 밑에 중첩한다. **`/rounds/{id}` 로 평평하게 두면 권한 검사를 위해
매번 차수→모임을 거꾸로 조회해야 한다.** 중첩하면 주최자 검사가 한 곳에서 끝난다.

### 4.1 차수 추가 — `POST /gatherings/{id}/rounds`

```
POST /gatherings/1/rounds
{
  "label": "3차 · 노래방",
  "total": 80000,
  "alcohol": 20000,
  "payerId": 3
}
```

```
201   Round
400   VALIDATION_FAILED       // ALCOHOL_EXCEEDS_TOTAL · PAYER_NOT_FOUND · AMOUNT_TOO_LARGE ...
409   GATHERING_CONFIRMED
```

**`seq` 는 클라이언트가 보내지 않는다.** 서버가 `MAX(seq)+1` 로 정한다.
클라이언트가 정하면 두 창에서 동시에 추가할 때 `DUPLICATE_ROUND_SEQ` 가 난다.

### 4.2 차수 수정 — `PATCH /gatherings/{id}/rounds/{roundId}`

```
PATCH /gatherings/1/rounds/2
{ "alcohol": 44000, "payerId": 2 }
```

`label` `total` `alcohol` `payerId` `seq` 를 바꿀 수 있다.

### 4.3 차수 삭제 — `DELETE /gatherings/{id}/rounds/{roundId}`

```
204
409   GATHERING_CONFIRMED
```

**그 차수의 `Attendance` 도 같이 지운다.** 남겨두면 계산에는 안 쓰이지만
`DRANK_WITHOUT_ATTEND` 같은 검증이 유령 데이터에 걸린다.

**`seq` 를 다시 매기지 않는다.** `1, 3, 4` 가 되어도 `label` 이 사람이 읽는 이름이고,
`seq` 는 정렬용이다. 다시 매기면 다른 사람이 보고 있던 화면의 번호가 바뀐다.

### 4.4 술병 — `PUT /gatherings/{id}/rounds/{roundId}/drink-items`

```
PUT /gatherings/1/rounds/2/drink-items
{
  "items": [
    { "name": "소주", "bottleCount": 4, "unitPrice": 5000 },
    { "name": "맥주", "bottleCount": 4, "unitPrice": 6000 }
  ]
}
```

```
200   Round                   // alcohol 이 44000 으로 덮어써진 상태로 돌아온다
400   VALIDATION_FAILED       // INVALID_DRINK_ITEM
```

**`PUT` 으로 통째 교체한다.** 개별 추가·삭제로 만들면 프론트가 3개 요청을 순서대로
보내야 하고, 중간에 실패하면 `alcohol` 이 어중간한 값으로 남는다.

**빈 배열을 보내면 술병이 전부 지워지고 `alcohol` 은 직접 입력값으로 돌아간다.**
`DrinkItem` 이 하나라도 있으면 `alcohol` 을 덮어쓰기 때문이다 (SPEC §4).

### 4.5 기타 항목

```
POST   /gatherings/{id}/extras                { label, amount, payerId, bearerIds[] }   → 201 ExtraItem
PATCH  /gatherings/{id}/extras/{extraId}      { label?, amount?, payerId?, bearerIds? } → 200 ExtraItem
DELETE /gatherings/{id}/extras/{extraId}                                                → 204
```

**`bearerIds` 는 통째로 교체한다.** 부담자 추가/제거를 따로 두면 "수아만 부담" →
"수아·지원 부담" 이 두 요청이 되고, 화면의 체크박스와 모양이 맞지 않는다.

**`bearerIds` 가 빈 배열이면 저장은 되고 확정에서 막힌다** (`NO_NON_EXEMPT_BEARER`).
택시비를 먼저 적고 누가 탔는지 나중에 고르는 순서를 허용해야 한다.

---

## 5. 참여자 — 링크로 들어오는 쪽

**토큰은 진입과 참여에만 쓴다. 참여한 뒤에는 `gatheringId` 로 부른다.**

이유가 둘이다. **`shareToken` 이 URL 에 적게 등장할수록 로그로 새는 경로가 줄어든다**
(SPEC §10 — 로그에 shareToken 을 남기지 말 것). 그리고 참여 이후의 권한은
토큰 소지가 아니라 **명단에 있는지**로 판단해야 한다 — 링크를 재발급하면
옛 토큰을 가진 참여자가 자기 정산에서 쫓겨나면 안 된다.

### 5.1 진입 — `GET /g/{token}`  (W0)

```
200
{
  "gatheringId": 1,
  "name": "8월 팀 회식",
  "date": "2026-08-14",
  "status": "COLLECTING",
  "hostName": "동규",
  "participantCount": 4,
  "expectedCount": 5,
  "rounds":  [ { "label": "1차 · 삼겹살집", "total": 137000 }, ... ],
  "extras":  [ { "label": "택시비", "amount": 21000 } ],
  "participants": [ { "name": "동규", "profileImage": "..." }, ... ],
  "myParticipantId": null
}
```

```
404   INVALID_SHARE_TOKEN
429   TOO_MANY_REQUESTS
```

**전체 `Gathering` 을 주지 않는다.** 아직 참여하지 않은 사람이고, 링크가 잘못
뿌려졌을 수도 있다. **"이거 내 모임 아니네"를 알아볼 만큼만** 준다 — 이름·날짜·
주최자·차수 목록·참여자 이름. 금액 배분과 `Attendance` 는 참여 후에 준다.

**`myParticipantId`** — 로그인한 상태로 부르면 이미 참여 중인지 알려준다.
링크를 다시 열었을 때 참여 버튼을 또 보여주면 안 된다.

> **로그인 없이도 호출된다.** 링크를 눌러 로그인 화면이 뜨기 전에 모임 이름을
> 보여줘야 하고(카카오톡 링크 카드가 이미 그것을 보여준 상태다), 로그인 후
> 돌아와 다시 부른다. 토큰 없이 부르면 `myParticipantId` 는 항상 `null`.

**`429` 를 두는 이유** — `shareToken` 은 이 리소스의 유일한 자격증명이다.
무작위 대입을 막지 않으면 남의 모임 이름과 참여자 명단이 새어 나간다.

### 5.2 참여 — `POST /g/{token}/join`  (W0-2)

```
POST /g/{token}/join
Authorization: Bearer <jwt>
{ "name": "봄이" }
```

```
201   { "participantId": 91, "status": "JOINED",  "gatheringId": 1 }
201   { "participantId": 91, "status": "PENDING", "gatheringId": 1 }   // 정원 초과
409   ALREADY_JOINED
409   GATHERING_CONFIRMED
404   INVALID_SHARE_TOKEN
```

**로그인이 필수다.** 남의 이름을 대신 누르는 것을 막는 유일한 장치다 (SPEC §5).

**정원을 넘으면 오류가 아니라 `PENDING` 이다.** 단톡방에 링크를 뿌리면 모르는
사람이 들어올 수 있고, 그렇다고 6번째 사람을 무조건 막으면 인원을 잘못 적은
주최자가 정산을 못 한다. **주최자 승인으로 넘긴다** (§6.3).

**`name` 은 참여자가 직접 적는다.** 카카오 닉네임(`🌸봄이🌸`)은 총무가 알아볼 수
없다. 기본값으로 채워주되 바꿀 수 있게 한다.

#### 동시성 — 반드시 이렇게

**조회와 삽입 사이가 벌어지면 뚫린다.** 정원 5명에 4명이 있을 때 두 사람이 같은
순간에 참여하면, 둘 다 `4 < 5` 를 읽고 둘 다 삽입해 6명이 된다.

```
BEGIN
  SELECT ... FROM gathering WHERE id = ? FOR UPDATE     ← 같은 모임의 참여를 직렬화
  참여자 수 세기 → 정원 판단
  INSERT INTO participant ...
COMMIT
```

`UNIQUE (gathering_id, user_id)` 도 함께 둔다. 잠금이 막는 것과 다른 경합
(같은 사람이 두 탭에서 동시에 누르는 것)을 막는다. 위반은 `409 ALREADY_JOINED` 로 변환한다.

> 참여자 수를 `gathering` 에 비정규화해 세지 않는다. 그 컬럼은 명단 제거·승인·거부
> 세 경로에서 같이 갱신돼야 하고, 한 곳을 빠뜨리면 **정원 게이트가 조용히 틀린다.**
> 자세한 것은 `ADR-008`.

### 5.3 내 정산 보기 — `GET /gatherings/{id}`  (W1 · W2)

주최자와 **같은 엔드포인트**다. 참여자가 부르면 응답이 같다 — 전원의 체크 내역과
금액을 모두에게 공개하는 것이 의도된 설계다 (SPEC §5 자기신고 신뢰).

```
200   Gathering                    // types.ts
403   NOT_FOUND                    // 명단에 없으면 404
```

### 5.4 체크 제출 — `PUT /gatherings/{id}/participants/{pid}/attendance`  (W1)

```
PUT /gatherings/1/participants/91/attendance
{
  "entries": [
    { "roundId": 1, "attended": true,  "drank": false },
    { "roundId": 2, "attended": false, "drank": false }
  ]
}
```

```
200   Gathering                    // 갱신된 전체를 돌려준다
400   VALIDATION_FAILED            // DRANK_WITHOUT_ATTEND
403   NOT_SELF                     // 본인도 주최자도 아님
409   GATHERING_CONFIRMED
```

**전체 차수를 한 번에 보낸다.** 차수별로 나누면 3차까지 체크한 사람이 3번 요청하고,
2번째에서 끊기면 절반만 저장된 상태가 남는다.

**`403 NOT_SELF` 이 아니라 통과하는 경우가 하나 있다 — 주최자다.** 미응답자를
주최자가 대신 체크할 수 있어야 한다. **잠수타는 사람이 반드시 나오고, 정산이
그 사람 손에 인질로 잡히면 서비스가 멈춘다** (SPEC §5).

**확정 후에는 `409` 다. 재시도를 요구하지 않는다.**

> `"이미 정산이 확정되었습니다."`
>
> 술자리 다음날 아침의 사용자에게 `"다시 시도해주세요"` 를 띄우는 것은 이탈이다
> (SPEC §5-c). 상태를 `UPDATE` 조건에 넣어 한 번에 처리하고, 갱신된 행이 0이면
> 재시도가 아니라 **명확한 종료**를 보여준다.

### 5.5 계좌 등록 — `PUT /gatherings/{id}/participants/{pid}/payout`

```
PUT /gatherings/1/participants/3/payout
{ "bankName": "카카오뱅크", "accountNo": "3333-01-2345678", "accountHolder": "이재훈" }
```

```
200   Participant
403   NOT_SELF                     // 남의 계좌를 등록할 수 없다
```

**결제자가 자기 계좌를 직접 등록한다.** 주최자가 다른 사람 계좌를 대신 넣게 하면
총무가 다시 모든 것을 하게 된다. 그리고 **주최자 말고도 받을 사람이 생긴다** —
택시비를 재훈이 결제했으면 수아·지원은 재훈에게 보낸다.

---

## 6. 명단 관리 — 주최자

### 6.1 면제자 지정 — `PATCH /gatherings/{id}/participants/{pid}`

```
PATCH /gatherings/1/participants/5
{ "exempt": true }
```

```
200   Gathering
403   NOT_HOST
409   GATHERING_CONFIRMED
```

`exempt` 와 `name` 을 바꿀 수 있다. **`name` 은 본인도 바꿀 수 있다** (`NOT_SELF` 통과).

### 6.2 명단에서 제거 — `DELETE /gatherings/{id}/participants/{pid}`

```
204
403   NOT_HOST
409   GATHERING_CONFIRMED
409   PARTICIPANT_HAS_PAYMENT      // 입금 표시된 사람
```

**입금 표시된 사람은 뺄 수 없다.** 지우면 `paidAmount` 가 사라져 얼마를 돌려줘야
하는지 알 수 없게 된다. 입금 상태를 먼저 해제하게 한다.

**주최자 자신은 뺄 수 없다** (`hostParticipantId`) — `403 NOT_HOST` 로 답한다.
**모임을 만든 사람이 결제자다.** 빠지면 결제한 돈의 주인이 없어져 송금 목록이 성립하지 않는다.

### 6.4 본인 탈퇴는 만들지 않는다

**참여자가 스스로 빠지는 경로를 두지 않는다.** 잘못 들어온 사람은
`DELETE /gatherings/{id}/participants/{pid}` 로 **주최자가 뺀다**(§6.2).

주최자는 애초에 빠질 수 없고(위), 참여자는 이미 주최자가 뺄 수 있으므로
탈퇴 엔드포인트는 **경로를 하나 더 만들 뿐 새로 되는 일이 없다.** 오히려
확정 직전에 한 명이 빠지면 전원 금액이 흔들리는데(분모가 바뀐다),
그 판단을 주최자에게 두는 것이 맞다.

### 6.3 참여 요청 승인·거부

```
POST /gatherings/{id}/participants/{pid}/approve      → 200 Gathering
POST /gatherings/{id}/participants/{pid}/reject       → 204
```

`PENDING` 인 참여자만 대상이다. **승인하면 `expectedCount` 를 같이 올린다** —
안 그러면 다음 사람도 또 `PENDING` 이 되어 주최자가 매번 승인한다.

---

## 7. 확정 — 두 단계

```
[확정하기] → GET  preview  → 금액 + inputHash 확인 → [수락] → POST confirm { inputHash }
```

### 7.1 미리보기 — `GET /gatherings/{id}/settlement/preview`  (H3-b)

```
200
{
  ...Settlement,                        // types.ts
  "inputHash": "a3f2c81e",
  "validationErrors": [],
  "unresponded": [ { "participantId": 5, "name": "지원" } ],
  "reconfirm": null
}
```

재확정이면 `reconfirm` 이 채워진다. **추측이 아니라 실제 수치를 보여준다** (SPEC §5).

```json
"reconfirm": {
  "additionalPaymentCount": 3, "additionalPaymentTotal": 9450,
  "refundCount": 1,            "refundTotal": 3000
}
```

**검증 실패해도 `400` 이 아니라 `200` 이다.** 미리보기는 *"확정하면 이렇게 된다"* 를
보여주는 화면이고, 무엇이 막고 있는지도 그 화면에서 보여줘야 한다.
`400` 으로 던지면 프론트가 금액을 하나도 못 그린다.

### 7.2 수락 — `POST /gatherings/{id}/confirm`

```
POST /gatherings/1/confirm
{ "inputHash": "a3f2c81e" }
```

```
200   Settlement                   // revision 이 오른 확정 결과
400   VALIDATION_FAILED            // CONFIRM 단계 검증
403   NOT_HOST
409   STALE_INPUT_HASH             // ↓
409   GATHERING_CONFIRMED
```

**`inputHash` 가 다르면 거부한다.** 두 단계로 나누면 **그 사이에 참여자가 체크를
바꿀 수 있다.** 그러면 주최자가 확인하고 동의한 금액과 실제로 확정되는 금액이
달라져 게이트를 만든 목적 자체가 무너진다 (ADR-004).

```json
{
  "code": "STALE_INPUT_HASH",
  "message": "수아님의 응답이 변경되었습니다. 금액을 다시 확인해주세요.",
  "errors": null
}
```

**여기서는 재확인을 요구해도 된다.** 주최자는 앱 앞에 있고, 금액이 바뀌었다면
마땅히 다시 봐야 한다. 참여자에게 재시도를 요구하지 않는 것(§5.4)과는 대상이 다르다.

**서버가 같이 하는 일** — `status = CONFIRMED`, `revision += 1`, `confirmedAt` 갱신,
그리고 **outbox 에 알림 이벤트를 같은 트랜잭션으로 적는다** (ADR-002).

### 7.3 확정 결과 — `GET /gatherings/{id}/settlement`  (H4 · W2)

```
200   Settlement
409   GATHERING_COLLECTING         // 아직 확정 전
```

**매번 다시 계산한다. 저장하지 않는다** (ADR-005). 근거 화면과 재정산이 여기 의존한다.

### 7.4 되돌리기 — `POST /gatherings/{id}/reopen`

```
200   Gathering                    // status = COLLECTING
403   NOT_HOST
409   GATHERING_COLLECTING
```

**경고에 쓸 숫자를 위한 별도 엔드포인트를 두지 않는다.** `GET /gatherings/{id}` 의
`participants[].paymentStatus` 로 프론트가 셀 수 있다.

> `4명이 이미 입금했습니다. 수정하면 전원의 금액이 바뀌어 추가 입금이나 환불이 발생합니다.`

`revision` 은 되돌릴 때 내리지 않는다. 확정 횟수를 세는 값이다.

---

## 8. 입금 상태 — `PUT /gatherings/{id}/participants/{pid}/payment`

```
PUT /gatherings/1/participants/3/payment
{ "status": "SENT" }
```

```
200   Gathering
403   NOT_SELF                     // 남의 상태를 바꾸려 함
409   GATHERING_COLLECTING         // 확정 전에는 입금이 없다
409   INVALID_TRANSITION
```

| 전이 | 누가 |
|---|---|
| `NONE → SENT` | 참여자 본인 · 주최자 |
| `SENT → NONE` | 참여자 본인 (`RECEIVED` 전까지) |
| `SENT → RECEIVED` | **주최자만** |
| `NONE → RECEIVED` | **주최자만** — 현금으로 받았거나 참여자가 표시를 안 한 경우 |
| `RECEIVED → NONE` | **주최자만** — 참여자는 해제할 수 없다 |

**`paidAmount` 는 클라이언트가 보내지 않는다.** `SENT`/`RECEIVED` 로 바뀌는 순간
**서버가 그 시점의 부담액을 계산해 박는다** (SPEC §5-b). 자기신고로 두면 조작
여지가 생기고, 이 값이 §5-c 차액 계산의 유일한 근거다.

```
차액(p) = 현재 최종부담액(p) − paidAmount(p)
  > 0  추가 입금 필요     < 0  환불 대상     = 0  완료
```

---

## 9. 화면 ↔ 엔드포인트

| 화면 | 부르는 것 |
|---|---|
| 로그인 | `POST /auth/kakao` · `GET /auth/me` |
| **H0** 내 모임 | `GET /gatherings` · `DELETE /gatherings/{id}` · `POST /restore` |
| **H1** 모임 만들기 | `POST /gatherings` |
| **H2** 금액 입력 | `GET /gatherings/{id}` · `POST·PATCH·DELETE /rounds` · `PUT /drink-items` · `POST·PATCH·DELETE /extras` |
| **H3** 수집 현황 | `GET /gatherings/{id}` · `PUT /attendance`(대신 체크) · `PATCH /gatherings/{id}` |
| **H3-b** 확정 미리보기 | `GET /settlement/preview` → `POST /confirm` |
| **H4** 결과·입금 | `GET /settlement` · `PUT /payment` · `POST /reopen` |
| **H5** 명단 | `PATCH·DELETE /participants/{pid}` · `approve` · `reject` · `POST /share-token` |
| **W0** 참여 | `GET /g/{token}` → `POST /g/{token}/join` |
| **W1** 체크 | `GET /gatherings/{id}` · `PUT /attendance` |
| **W2** 내 결과 | `GET /settlement` · `PUT /payment` · `PUT /payout` |
| **W3** 전체 내역 | `GET /gatherings/{id}` · `GET /settlement` |

**엔드포인트 24개.** 지금 `api.ts` 에 있는 것은 이 중 **읽기 7개**뿐이다.

`H0` 에는 삭제·되돌리기 버튼이 아직 없다. `deleteScheduledAt` 배지와 함께
화면을 만들어야 한다 — `types.ts` 의 `Gathering`·`GatheringSummary` 에도
그 필드를 넣는다.

---

## 10. 결정된 것과 남은 것

### 결정됨

| 항목 | 값 | 근거 |
|---|---|---|
| 갱신 토큰 | **만들지 않는다** | 닉네임·프로필·금액뿐이고 송금을 실행하지 않는다 (§2.3) |
| 토큰 만료 | 14일 | 한 모임의 생애를 덮는 길이 |
| 모임 삭제 | **2트랙 · 7일 유예** | 화면 확인 → 삭제 예정 → 7일 뒤 실제 삭제 (§3.5) |
| 본인 탈퇴 | **만들지 않는다** | 주최자는 결제자라 빠질 수 없고, 참여자는 주최자가 뺀다 (§6.4) |

### 남음

| # | 항목 | 걸리는 점 |
|---|---|---|
| 1 | `429` 임계값 | `shareToken` 길이와 함께 정해야 한다 — 길이가 근본이고 `429` 는 보조다 |
| 2 | OG 응답 | `/g/{token}` 을 백엔드가 HTML로도 서빙해야 한다 (`ADR-007`) |

**둘은 서로 얽혀 있다.** OG 를 위해 `/g/{token}` 을 백엔드가 받게 되면
`429` 를 걸 지점도 그쪽으로 옮겨간다. `ADR-007` 에서 함께 정한다.

---

## 11. 이 문서와 SPEC 의 차이

**SPEC §4 의 `Gathering` 에 아직 `payoutBankName` 등이 붙어 있다.** 계좌는
`Participant` 로 옮기기로 했다 — 주최자 말고도 받을 사람이 생기기 때문이다(§5.5).
`types.ts` 는 이미 옮긴 형태이고, **이 계약도 옮긴 형태를 따른다.**

SPEC 갱신 때 §4 를 함께 고친다.
