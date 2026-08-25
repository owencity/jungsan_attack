# 정산어택 — API 계약 · v3

> **v3 변경(2026-08-26)** — **구현이 계약을 앞지른 부분을 문서에 반영했다.**
> 이 문서는 "합의하는 형식"인데, 인증을 먼저 구현하면서 계약과 어긋난 채로
> 두면 웹·앱 클라이언트가 잘못된 문서를 보고 만들게 된다.
>
> | 절 | 바뀐 것 |
> |---|---|
> | §0, §2 | 인증이 `Authorization: Bearer` → **httpOnly 쿠키** |
> | §2.1 | 로그인이 `POST {code}` → **서버사이드 리다이렉트** |
> | §2.3 | 토큰 저장이 localStorage → 쿠키. CSRF 대가를 명시 |
> | §3-b | 모임에 `groupType`(FLASH/RECURRING). 번개는 술자리 1개를 함께 생성 |
> | §1.4 | `FLASH_GROUP_HAS_GATHERING` 추가 |
>
> **채팅(§5-b)은 손대지 않았다** — `ADR-014` 로 MVP 범위에서 빠졌지만,
> 설계는 그대로 유효하고 발동 조건이 오면 쓴다.
>
> ⚠️ **아직 남은 불일치** — §2.3 토큰 만료가 문서 14일 / 구현 30일이다.
> 그리고 이 문서 전반이 `Gathering`을 "모임"이라 부르는데,
> 지금 용어는 **모임=`Group` · 술자리=`Gathering`** 이다(DB 코멘트는 정리됨).

> **v2 변경** (`ADR-009`~`013`) — `Group`(영구 모임)·실시간 채팅·MSA(k3s) 를
> 반영했다. 기존 번호(§0~11)는 그대로 두고 `SPEC.md` v4 와 같은 방식으로
> `§3-b`·`§5-b` 를 새로 달았다 — 이 문서를 참조하는 곳이 `ADR-008` 하나뿐이라
> 재번호해도 위험은 작았지만, 일관성을 위해 같은 규칙을 썼다.
>
> 이 v2 는 **정산(Gathering) 도메인의 기존 24개 엔드포인트를 하나도 바꾸지
> 않는다.** `Group`은 얹는 것이고, `Gathering`은 `Group` 없이도 그대로 유효하다
> (`ADR-009`) — 그래서 `/gatherings/*` 를 `/groups/{id}/gatherings/*` 로
> 옮기지 않았다. 처음 이 재구성을 이야기할 때 그렇게 옮기겠다고 했었는데,
> 그러면 `Group` 없는 `Gathering`을 표현할 경로가 없어진다 — 그 계획을 취소했다.

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
| 시각 | ISO-8601 + offset (`2026-08-17T21:40:00+09:00`) | 서버는 UTC로 쓰고 읽는다(`ERD.md` §0). 직렬화 형식은 DB 컬럼 타입과 무관 |
| 날짜 | `YYYY-MM-DD` (모임 날짜) | 시각이 아니다. UTC 변환 금지 — ↓ §0.2 |
| 인증 | **httpOnly 쿠키** (`jeongsan_token`) | ↓ §2 |
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

### 0.3 이 문서의 `/api/v1/...` 뒤에는 서비스가 3개 있다 (`ADR-013`)

**프론트는 이 사실을 몰라도 된다.** Spring Cloud Gateway가 경로로 갈라 보낸다.

```
/api/v1/auth/**                                   REST API 서비스
/api/v1/gatherings/**  · /api/v1/groups/**         REST API 서비스
/api/v1/g/{token}  · /api/v1/gr/{token}            REST API 서비스
/ws/gatherings/{id}                                채팅 게이트웨이 (§5-b)
/api/v1/gatherings/{id}/messages                   채팅 게이트웨이 (§5-b) — REST 지만 메시지는 MongoDB 에 있다
```

이 문서의 나머지 절에서 "어느 서비스가 처리하는지"는 §5-b에서만 명시한다.
그 외에는 전부 REST API 서비스다.

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
| `NOT_GROUP_OWNER` | 403 | 그룹 관리 기능은 OWNER 전용 (§3-b) |
| `ALREADY_GROUP_MEMBER` | 409 | 이미 그룹 멤버 (§3-b) |
| `INVALID_GROUP_TOKEN` | 404 | 그룹 초대 링크가 틀리거나 재발급됨 (§3-b) |
| `FLASH_GROUP_HAS_GATHERING` | 409 | 번개 모임에는 술자리를 하나만 둔다 (§3-b.2) |

---

## 2. 인증

> **v3 변경(2026-08-26)** — 이 절은 **구현에 맞춰 다시 썼다.**
> 원래 계약은 프론트가 카카오 SDK 로 인가코드를 받아 `POST /auth/kakao` 로 넘기면
> 서버가 `{token, user, firstLogin}` 을 **body 로** 주는 방식이었다.
> 실제 구현은 **서버사이드 리다이렉트 + httpOnly 쿠키**로 갔다. 이유는 §2.3 참조.

### 2.1 로그인 — `GET /api/v1/auth/kakao/login`

**브라우저를 이 주소로 보내면 된다.** 프론트는 카카오 SDK 를 붙이지 않는다.

```
GET /api/v1/auth/kakao/login
302 → https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...
```

사용자가 카카오에서 로그인·동의하면 카카오가 아래로 되돌려보낸다.

```
GET /api/v1/auth/kakao/callback?code=3xY...
302 → {app.login-success-url}
Set-Cookie: jeongsan_token=eyJ...; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=2592000
```

**서버가 하는 일** — 인가코드를 카카오 토큰으로 교환하고, 사용자 정보를 조회해
`users` 를 UPSERT(`provider` + `providerId` 유니크)한 뒤, 자체 JWT 를 쿠키로 내린다.

**`redirectUri` 를 프론트가 보내지 않는다.** 콜백이 **서버 자신의 주소**라
환경변수(`KAKAO_REDIRECT_URI`) 하나로 끝난다. 원래 계약은 프론트가 이 값을 보내는
전제여서 화이트리스트 검증이 필요했는데, 이 방식에서는 그 공격면 자체가 없다.

**`code`·클라이언트 시크릿은 서버만 다룬다.** 프론트는 아무것도 다루지 않는다.

> **`firstLogin` 이 없어졌다.** 리다이렉트에는 body 가 없기 때문이다.
> 최초 로그인 안내가 필요해지면 `GET /auth/me` 응답에 얹거나
> 리다이렉트 URL 에 쿼리 파라미터로 붙인다. **아직 정하지 않았다.**

> **구글은 아직 없다.** `users.provider` 에 자리는 있으나 구현은 카카오뿐이다.

**`firstLogin` 을 주는 이유** — 최초 로그인이면 화면이 안내를 더 보여줄 수 있다.
UPSERT 결과를 서버는 알지만 프론트는 알 수 없다.

### 2.2 내 정보 — `GET /api/v1/auth/me`

```
GET /api/v1/auth/me
Cookie: jeongsan_token=eyJ...          ← 브라우저가 자동으로 붙인다

200   { "id": 12, "nickname": "🌸봄이🌸", "profileImageUrl": "https://..." }
401   쿠키가 없거나 서명이 안 맞거나 만료됨
```

**새로고침할 때마다 프론트가 이걸 부른다.** 쿠키가 httpOnly 라 JS 가 읽을 수 없어
**로그인 여부를 프론트가 스스로 알 방법이 없다** — 서버에 물어봐야 한다.

**응답에 닉네임·프로필이 있는 이유** — 토큰에는 `userId` 만 담기 때문이다(§2.3).
화면에 이름을 보여주려면 매번 DB 에서 읽어야 하고, 그래야 카카오에서 닉네임을
바꿨을 때 바로 반영된다.

### 2.3 토큰 정책

```
방식        JWT (HS256) · httpOnly 쿠키 (jeongsan_token)
저장        브라우저 쿠키 저장소 — JS 는 읽을 수 없다
만료        14일  ⚠️ 구현은 현재 30일 (JWT_EXPIRATION_DAYS 기본값) — 아래 참조
갱신        없다 — 만료되면 다시 로그인
담는 것     sub(userId), iat, exp        ← 닉네임·프로필·provider 는 담지 않는다
쿠키 속성    HttpOnly · SameSite=Lax · Secure(운영만) · Path=/
```

**쿠키로 바꾼 이유** — 원래는 localStorage 였다. 크로스 사이트 쿠키 설정이
번거롭다는 게 근거였는데, **API 를 `api.jungsan.devkdk.com` 서브도메인에 두면
same-site 가 되어 그 문제가 사라진다.** SameSite 판정은 등록 도메인(`devkdk.com`)
기준이라 서브도메인끼리는 같은 사이트로 본다.

그래서 `SameSite=None` 도, 그에 딸린 복잡함도 필요 없어졌고, **XSS 로 토큰을
훔칠 수 없다는 이득만 남았다.** localStorage 는 스크립트가 읽을 수 있지만
httpOnly 쿠키는 읽을 수 없다.

> **대가는 CSRF 다.** 쿠키가 자동으로 실리므로 상태를 바꾸는 요청은 보호가 필요하다.
> `SameSite=Lax` 가 크로스 사이트 POST 를 막아주지만 **완전한 방어는 아니다.**
> 지금은 CORS `allowedOrigins` 를 프론트 origin 하나로 좁혀둔 상태다.
> **CSRF 토큰은 아직 없다 — 결제나 송금을 붙이는 날 반드시 다시 봐야 한다.**

> **웹이 아닌 클라이언트(안드로이드·iOS)** 는 쿠키를 쓰기 번거로울 수 있다.
> 그때는 같은 JWT 를 `Authorization: Bearer` 로도 받도록 서버를 넓히면 된다 —
> 토큰 형식이 같아서 발급 로직은 그대로다. **아직 구현하지 않았다.**

**갱신 토큰을 만들지 않는다 (확정).** refresh 토큰은 저장·회전·폐기 설계가 딸려온다.
**이 앱이 들고 있는 것은 닉네임·프로필 사진·모임 금액뿐이고 송금을 실행하지 않는다.**
그 정보량에 refresh 토큰 수명주기를 붙일 이유가 없다.

**1년에 몇 번 쓰는 앱**에서 14일마다 카카오 버튼을 한 번 더 누르는 비용이 그보다 작다.
카카오톡 인앱 브라우저에서는 이미 로그인돼 있어 탭 두 번이면 끝난다.

> 14일은 **한 모임의 생애를 덮는 길이**로 잡은 것이다. 술자리 다음날 정산하고,
> 입금 확인이 며칠에 걸쳐 끝난다. 그보다 짧으면 정산 도중에 튕기고,
> 갱신이 없으므로 길게 잡을수록 탈취된 토큰이 오래 살아 있다.

> ⚠️ **문서와 구현이 어긋나 있다.** 구현(`application.yml` 의
> `JWT_EXPIRATION_DAYS` 기본값)은 **30일**이다. 근거를 갖고 고른 값이 아니라
> 구현할 때 임의로 넣은 값이다. 위 14일 논거가 여전히 맞다면 구현을 14로 내려야 한다.
> **아직 정하지 않았다.**

**닉네임을 토큰에 담지 않는 이유** — 사용자가 카카오에서 닉네임을 바꾸면
토큰 안의 값이 만료될 때까지 낡은 채로 남는다.

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
  "groupId": 100,
  "payout": { "bankName": "국민은행", "accountNo": "123456-78-901234", "accountHolder": "김동규" }
}
```

```
201   Gathering                   // shareToken 포함. 프론트는 이걸로 링크를 만든다
400   VALIDATION_FAILED           // INVALID_ROUNDING_UNIT 등
403   NOT_GROUP_OWNER              // groupId 를 보냈는데 그 그룹 멤버가 아님
```

| 필드 | 필수 | 비고 |
|---|---|---|
| `name` | ✔ | |
| `date` | ✔ | |
| `hostName` | ✔ | 주최자의 `Participant.name`. 닉네임이 기본값이지만 바꿀 수 있다 |
| `expectedCount` | ✔ | **정원 초과 승인 게이트의 기준.** 비면 방어가 통째로 사라진다 |
| `roundingUnit` | | 없으면 10 |
| `groupId` | | 영구 그룹에 묶는다(`ADR-009`). 없으면 1회성 모임 |
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

#### shareToken 형식 — 12자 · 62종

```
길이       12자
문자집합   [0-9A-Za-z]           62종
생성       SecureRandom          ← java.util.Random 금지
컬럼       VARCHAR(12) UNIQUE NOT NULL
```

**이 토큰이 `GET /g/{token}` 의 유일한 자격증명이다.** 로그인 없이 응답하는
주소가 이것뿐이므로, 짧으면 무작위로 맞춰서 남의 모임 명단을 긁어갈 수 있다.

```
7자 · 36종     36^7  = 7.8 × 10^10     모임 1만 개면 초당 1,000회로 1.5시간
12자 · 62종    62^12 = 3.2 × 10^21     같은 조건에서 5,100만 년
```

**`SecureRandom` 을 쓴다.** `java.util.Random` 은 시드 48비트라 몇 개만 관측하면
다음 값을 예측할 수 있다. 길이를 늘려도 예측 가능하면 의미가 없다.

**길이가 근본이고 `429`(§5.1)는 보조다.** 토큰이 짧으면 `429` 로 메울 수 없다 —
분산해서 던지면 IP 당 제한은 IP 를 늘려 피한다.

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

## 3-b. 모임 — `Group` (영구 모임, `ADR-009`)

`Group`은 `Gathering`과 **같은 패턴**을 쓴다 — 초대 링크·참여·역할. 참여자가
이미 `/g/{token}` 흐름에 익숙하므로 같은 모양을 재사용하면 새로 배울 게 없다.

**다만 이번 버전에서는 얇게 간다.** `ADR-009`가 *"Group은 참여를 자동화하지
않는다"*까지만 정했고, 초대 링크의 OG 카드 최적화(`ADR-007`이 `Gathering`에
한 것과 같은 수준)는 여기서 하지 않는다 — Group은 카카오톡으로 낯선 사람에게
뿌리는 것이 아니라 **이미 아는 사람들끼리** 쓰는 것이라 우선순위가 낮다.
필요해지면 그때 `ADR-007`과 같은 패턴을 적용한다.

> **v3 추가(2026-08-26) — 모임에 두 종류가 생겼다.**
>
> | | `RECURRING` (주기) | `FLASH` (번개) |
> |---|---|---|
> | 뜻 | 계속 만나는 고정 멤버 | 1회성 |
> | 소속 술자리 | 여러 개 | **딱 1개** (더 못 만든다) |
> | 수명 | 무한 | **정산 확정 +14일 뒤 목록에서 사라짐** |
>
> 새 테이블을 만들지 않고 `groups.group_type` 컬럼 하나로 처리한다 —
> `ADR-009` 가 "이름·구조를 바꾸지 않는다"고 못박은 이유가 여기도 적용된다.

### 3-b.1 목록 — `GET /groups`

```
200
[
  { "id": 100, "name": "신림팸", "groupType": "RECURRING",
    "memberCount": 4, "gatheringCount": 3 },
  { "id": 101, "name": "8/26 번개", "groupType": "FLASH",
    "memberCount": 3, "gatheringCount": 1 }
]
```

**소프트 삭제된 모임은 빠진다** (`deleted_at IS NULL`). 목록에서만 사라질 뿐
결제 내역(id 로 직접 접근)에서는 계속 보인다 — 지난 정산 기록이 증발하면 안 된다.

### 3-b.2 생성 — `POST /groups`

```
POST /groups
{ "name": "신림팸", "groupType": "RECURRING" }
```

```
201   { "id": 100, "name": "신림팸", "groupType": "RECURRING",
        "shareToken": "aB3xY9...", "role": "OWNER", "gatheringId": null }
```

**번개는 술자리까지 함께 만든다.**

```
POST /groups
{ "name": "8/26 번개", "groupType": "FLASH",
  "gatheringDate": "2026-08-26", "expectedCount": 5 }
```

```
201   { "id": 101, "name": "8/26 번개", "groupType": "FLASH",
        "shareToken": "kR7mQ2...", "role": "OWNER", "gatheringId": 55 }
```

**서버가 같이 하는 일**

1. 생성자를 `GroupMember(role=OWNER)` 로 즉시 등록한다 — 주인 없는 그룹은 없다
   (`Gathering` 의 `hostParticipantId` 와 같은 이유).
2. `groupType=FLASH` 면 **같은 트랜잭션에서 `Gathering` 을 1개 만들고** 그 id 를
   `gatheringId` 로 돌려준다. 번개는 "오늘 한 번" 쓰는 것이라 모임을 만들고
   술자리를 또 만들라고 하면 번거롭다 — 화면 한 단계를 줄인다.

| 필드 | RECURRING | FLASH |
|---|---|---|
| `gatheringDate` | 무시된다 | **필수** |
| `expectedCount` | 무시된다 | **필수** — 본인 포함 예상 인원 |
| 응답 `gatheringId` | `null` | 생성된 술자리 id |

> **`expectedCount` 를 여기서 받는 이유** — `gatherings.expected_count` 가 NOT NULL 이고,
> 정원 초과 시 참여 승인 게이트(§5.2)의 기준이 되는 값이다. 임의의 기본값을 넣으면
> 그 게이트가 무의미해지므로 만들 때 함께 받는다.

> **FLASH 모임에는 술자리를 더 못 만든다.** `POST /gatherings` 에 `groupId` 로
> FLASH 모임을 지정하면 `409 FLASH_GROUP_HAS_GATHERING` 을 돌려준다.
> 1회성이라는 정의가 곧 이 제약이다.

### 3-b.3 상세 — `GET /groups/{id}`

```
200
{
  "id": 100, "name": "신림팸",
  "members": [ { "userId": 7, "nickname": "동규", "role": "OWNER" }, ... ],
  "gatherings": [ { "id": 1, "name": "8월 팀 회식", "date": "2026-08-14" }, ... ]
}
403   NOT_FOUND                    // 멤버가 아니면 404(§1.3 의 403/404 규칙과 동일)
```

`gatherings`는 `GET /gatherings?groupId=100`과 같은 목록을 편의상 함께 준다 —
H0 화면이 그룹 상세를 열자마자 소속 모임을 보여줘야 하므로 왕복을 줄인다.

### 3-b.4 초대 — `GET /gr/{token}` → `POST /gr/{token}/join`

`Gathering`의 `/g/{token}` → `/g/{token}/join`과 **완전히 같은 2단계**다.

```
GET /gr/{token}
200   { "groupId": 100, "name": "신림팸", "ownerName": "동규", "memberCount": 4 }
404   INVALID_GROUP_TOKEN

POST /gr/{token}/join
Cookie: jeongsan_token=<jwt>     # 브라우저가 자동으로 붙인다
201   { "groupId": 100, "role": "MEMBER" }
409   ALREADY_GROUP_MEMBER
404   INVALID_GROUP_TOKEN
```

**정원 개념이 없다.** `Gathering`의 `PENDING`(§5.2)은 `expectedCount`가 있어야
성립하는데, `Group`은 그런 상한을 두지 않는다 — 계속 만나는 사람들이 알아서
드나드는 것을 굳이 승인제로 만들 이유가 없다.

**요청 제한은 `Gathering`과 같은 규칙을 쓴다**(분당 30회, 연속 404 5회 → 10분,
§5.1). 로그인 없이 응답하는 주소가 하나 더 늘었을 뿐 위협 모델은 같다.

### 3-b.5 멤버 제거 — `DELETE /groups/{id}/members/{userId}`

```
204
403   NOT_GROUP_OWNER
```

**OWNER 자신은 못 뺀다** — `Gathering`의 주최자와 같은 이유(§6.2)로, 주인
없는 그룹을 만들지 않는다. OWNER 이전(양도) 기능은 이번 버전에 없다.

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

### 5.0 같은 경로가 두 호스트에 있다

`/g/{token}` 은 **호스트에 따라 다른 것을 응답한다.** `ADR-007` 이 정한 것이다.

| 호스트 | 응답 | 누가 부르나 |
|---|---|---|
| `join.devkdk.com/g/{token}` | **HTML** — `og:*` 태그 + 프론트로 보내는 스크립트 | 카카오톡 크롤러 · 링크를 누른 사람 |
| `jungsan.devkdk.com/g/{token}` | 정산어택 화면 (Vercel) | 리다이렉트로 도달한 사람 |
| `{api호스트}/api/v1/g/{token}` | **JSON** (↓ §5.1) | 그 화면의 JS |

**단톡방에 뿌리는 주소는 `join.devkdk.com` 이다.** 프론트가 만드는 공유 링크도
그 주소여야 한다 — 주소창에서 복사한 `jungsan.devkdk.com/g/…` 를 뿌리면
OG 를 거치지 않아 카드가 안 뜬다.

**HTML 쪽도 아래 §5.1 의 요청 제한을 똑같이 받는다.** 로그인이 필요 없어
오히려 더 쉬운 표적이다.

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

#### 요청 제한 — 틀린 요청만 센다

`shareToken` 은 이 리소스의 유일한 자격증명이다(§3.4). 무작위 대입을 막지 않으면
남의 모임 이름과 참여자 명단이 새어 나간다.

```
IP 당 요청        분당 30회
연속 404          5회 → 그 IP 를 10분 차단
성공(200) 응답    세지 않는다
```

**임계값은 정상 사용을 세서 정했다.**

```
카톡에서 링크 클릭      1회      모임 이름·참여자 확인
로그인하고 돌아옴       1회      myParticipantId 확인
참여 버튼              0회      POST join 은 다른 경로
참여 후 체크·결과       0회      gatheringId 로 옮겨간다 (§5)
                     ────
                     2~3회     → 분당 30회는 정상 사용의 10배
```

**성공 응답까지 세면 정상 사용자가 막힌다.** 단톡방에 링크를 뿌리면
**같은 사무실 20명이 NAT 뒤 같은 공인 IP** 로 들어오고, 카카오톡 인앱
브라우저는 미리보기까지 요청한다. 뒤에 클릭한 사람이 `429` 를 받게 된다.

**세야 하는 것은 요청 수가 아니라 틀린 요청 수다.** 정상 사용자는 `404` 를 보지 않는다.
연속 `404` 는 맞추려는 시도에서만 쌓인다.

**제한을 두 겹으로 나눈다** (`ADR-007`).

```
Cloudflare Rate Limiting    IP 당 분당 30회        우리 서버에 닿기 전에 끊긴다
Bucket4j (SPEC §10)         연속 404 5회 → 10분    토큰 유효성을 알아야 판단된다
```

**연속 `404` 규칙은 Cloudflare 가 대신할 수 없다.** 토큰이 유효한지는 DB 를 봐야
알고, 그건 애플리케이션만 할 수 있다. 반대로 **거친 상한을 애플리케이션에서 걸면
이미 늦다** — 요청이 VM 까지 도달해 커넥션과 스레드를 먹는다.

### 5.2 참여 — `POST /g/{token}/join`  (W0-2)

```
POST /g/{token}/join
Cookie: jeongsan_token=<jwt>     # 브라우저가 자동으로 붙인다
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

## 5-b. 실시간 채팅 (`ADR-010`)

**여기부터는 REST API 서비스가 아니라 채팅 게이트웨이(Netty)가 처리한다**
(`ADR-013`, §0.3). 메시지는 MongoDB Atlas에 있다 — MySQL 트랜잭션과 무관하다.

### 5-b.1 이력 조회 — `GET /gatherings/{id}/messages`

WebSocket에 붙기 전, 방에 처음 들어왔을 때(또는 재연결 시) 이전 메시지를
가져오는 REST 엔드포인트다.

```
GET /gatherings/1/messages?before=2026-08-20T21:00:00Z&limit=50
200
[
  { "type": "TEXT", "memberId": 7, "message": "2차 어디감?", "createdAt": "..." },
  { "type": "EXPENSE", "memberId": 3, "message": "84,000원의 지출을 등록했습니다", "createdAt": "..." }
]
403   NOT_FOUND                    // 명단에 없으면 404 (§1.3 규칙과 동일)
```

**`before` 커서 페이지네이션이다.** 채팅은 `OFFSET`이 아니라 시각 기준으로
과거로 스크롤하는 게 자연스럽고, MongoDB 인덱스(`meetingId, createdAt`)와도 맞는다.

### 5-b.2 WebSocket — `wss://{api호스트}/ws/gatherings/{id}`

```
핸드셰이크    Sec-WebSocket-Protocol: bearer.<JWT>
             ⚠ 쿼리 파라미터로 토큰을 보내지 않는다 — 로그에 남는다(ADR-010)

프레임(양방향)
  { "type": "TEXT", "message": "2차 어디감?" }              클라이언트 → 서버
  { "type": "TEXT", "memberId": 7, "message": "...",
    "createdAt": "..." }                                   서버 → 클라이언트(브로드캐스트)
```

**클라이언트는 `memberId`·`createdAt`을 보내지 않는다.** 서버가 핸드셰이크에서
검증한 JWT로 채우고, 시각도 서버가 찍는다 — 클라이언트 시계를 믿지 않는다.

**`EXPENSE`·`SETTLEMENT`·`JOIN`·`LEAVE`·`NOTICE` 타입은 클라이언트가 보내지
않는다.** 서버 내부 이벤트(지출 등록, 입장/퇴장)가 Kafka를 거쳐 이 방으로
발행하는 것이다(`ADR-010`) — 채팅 클라이언트는 받기만 한다.

### 5-b.3 접속 상태

```
GET /gatherings/{id}/presence
200   { "onlineMemberIds": [7, 3, 91] }
```

Redis `meeting:{id}:online`을 그대로 읽는다(`ADR-010`). REST로도 노출하는
이유는 WebSocket 연결 전에도(방에 들어가기 전 미리보기 등) 현재 접속자 수를
보여줄 수 있어야 하기 때문이다.

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
| **(신규)** 그룹 목록·상세 | `GET /groups` · `GET /groups/{id}` |
| **(신규)** 그룹 만들기 | `POST /groups` |
| **(신규)** 그룹 초대 | `GET /gr/{token}` → `POST /gr/{token}/join` · `DELETE /members/{userId}` |
| **(신규)** 채팅방 | `GET /messages` · `WS /ws/gatherings/{id}` · `GET /presence` |

**엔드포인트 33개** — 정산(Gathering) 도메인 24개(§3~8, 변동 없음) +
`Group` 6개(§3-b) + 채팅 3개(§5-b). 지금 `api.ts` 에 있는 것은 정산 도메인
**읽기 7개**뿐이다. `Group`·채팅은 프론트 화면이 아직 없다 — **계약만 있고
UI는 없는 상태**(SPEC v4 §9가 이걸 알고 있다: `Group` 통계·활동 타임라인·
채팅 검색을 명시적으로 범위 밖에 뒀다. 여기 나열한 것은 그 밖의 최소 기능이다).

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
| `shareToken` | **12자 · 62종 · SecureRandom** | 길이가 근본 방어다. 발급 전이라 지금이 가장 싸다 (§3.4) |
| 요청 제한 | **분당 30회 · 연속 404 5회 → 10분** | 성공은 세지 않는다. NAT 뒤 20명이 막히면 안 된다 (§5.1) |
| OG 응답 | **백엔드가 HTML 로 응답** | `join.devkdk.com/g/{token}`. UA 판별을 없앤다 (`ADR-007`) |
| DB 엔진 | **MySQL 8.4** | `ADR-011`. 이 문서의 타입 표기(§0)는 그대로 유효 — `BIGINT`·직렬화 형식은 엔진 무관 |
| `Group` 초대 | **`Gathering`과 같은 패턴, OG 최적화는 생략** | 아는 사람끼리 쓰는 것이라 우선순위 낮음 (§3-b) |
| 채팅 인증 | **WebSocket 서브프로토콜로 JWT 전달** | 쿼리 파라미터는 로그에 남는다 (`ADR-010`, §5-b.2) |
| MSA 라우팅 | **경로 기준으로 Gateway 가 분기** | REST API 서비스 / 채팅 게이트웨이 2곳뿐 (`ADR-013`, §0.3) |

### 남음

**계약 수준에서 미결인 것은 없다.** 남은 것은 구현 순서다.

| # | 항목 | 어디서 |
|---|---|---|
| 1 | 참여 동시성의 잠금 범위 | `ADR-008` |
| 2 | 테이블·컬럼·인덱스 | ✅ 완료 — Liquibase · `ERD.md` · `table-spec.xlsx`, 실제 MySQL 실행 검증까지 끝남 |
| 3 | `deleteScheduledAt` 배지와 삭제 버튼 | `H0` 화면 |
| 4 | `Group`·채팅 화면 | 계약(§3-b·§5-b)만 있고 프론트 화면이 아직 없다 |
| 5 | Spring Boot 서버 모듈 자체 | `settings.gradle.kts`에 `:server` 없음 — 다음 작업 |

---

## 11. 이 문서와 SPEC 의 차이

**SPEC §4 의 `Gathering` 에 아직 `payoutBankName` 등이 붙어 있다.** 계좌는
`Participant` 로 옮기기로 했다 — 주최자 말고도 받을 사람이 생기기 때문이다(§5.5).
`types.ts` 는 이미 옮긴 형태이고, **이 계약도 옮긴 형태를 따른다.**

SPEC 갱신 때 §4 를 함께 고친다. (`SPEC.md` v4 상단의 "누적된 미반영 변경
목록"에 이 항목이 이미 들어 있다 — 이 v2 개정도 그 목록을 다루지 않았다.)
