# 2026-10-04 — v3 API: 프론트가 붙이는 계약 한 장

출시 일정(`docs/RELEASE_2026_10_11.md`)에 맞춰 웹·Android·iOS가 목데이터를 API로 바꾼다. 세 플랫폼은 이미
목데이터로 모든 화면이 돈다 — **이 문서의 응답 모양이 곧 프론트 모델(웹 `v3/model.ts`, 앱 `v3/Model.kt`)이다.**
모양이 같으면 프론트는 목데이터를 API 호출로 바꾸기만 하면 된다.

- 경로·이름은 백엔드가 바꿔도 된다. **필드 의미와 규칙(권한·거절 조건·알림)은 지켜 달라.** 바꾸면 이 문서를 같이 고친다.
- 금액은 정수 원, 시각은 ISO-8601 UTC 문자열, id 는 number(API.md §0.1).
- 오류는 API.md §1.2 모양 그대로. 프론트는 `code`로 문구를 고른다.
- 앞선 FC 항목의 내용을 다시 쓰지 않는다 — 해당 항목 번호를 적는다.

## FC-014 v3 API 계약 — 화면별 엔드포인트·응답 모양·우선순위

- **상태:** 열림 — D1~D7 모두 추천안으로 결정(CTO, 2026-10-05)
- **바뀐 흐름:** 없음. 지금 화면 그대로를 API로 받친다.
- **백엔드 영향:** 아래 전부. 우선순위 P0 = 이것 없이는 앱 심사용 빌드를 못 만든다. P1 = 출시 전 필요. P2 = 출시 뒤 가능.

### 0. 결정 (CTO, 2026-10-05 — 아래 "추천" 열 그대로 확정)

| # | 질문 | 추천 | 막는 것 |
|---|---|---|---|
| D1 | **H1 목록이 무엇을 주나**(FC-001) | **D. 상세와 같은 `Gathering`을 배열로 준다.** 일회용·7일 삭제라 한 사람의 활성 술자리는 몇 개뿐이다. 프론트의 "지금 할 일"·뱃지 규칙(세 플랫폼에서 테스트 중)을 그대로 쓰고 서버는 요약 규칙을 따로 만들지 않는다. 대가: 목록 응답이 크다(술자리당 수 KB). 느려지면 그때 요약으로 줄인다 | H1 |
| D2 | **송금 근거**(FC-003) | **A. 정산 시점 스냅샷** + 근거는 "올림 전 금액", 1원 차이는 마지막 차수에 붙인다(합 = 송금액) | P3 근거 펼치기 |
| D3 | **총무 대리 응답을 참여자가 고칠 수 있나**(FC-004) | **A. 고칠 수 있다.** 잠그는 건 `EXEMPT`+`HOST`뿐 | R3 대리 응답 |
| D4 | **차수 이름**(FC-008) | **A. 서버는 `seq`만**, 이름(`N차`)은 프론트 | R2 |
| D5 | **정산금액 봤음**(FC-006) | **A. `participants.settlement_viewed_at`** | H1 [정산금액 확인] 뱃지 |
| D6 | **Apple 로그인**(App Store 심사 4.8) | **넣는다.** 카카오만 있으면 "이름·이메일만 받고 이메일을 숨길 수 있는 동등한 로그인"이 없어 반려될 수 있다. Sign in with Apple 이 그 조건을 만족하는 사실상 유일한 수단이다 | iOS 심사 |
| D7 | **탈퇴 조건**(App Store 심사 5.1.1(v), REQUIREMENTS §9 미결) | **진행 중(`OPEN`·`SETTLING`)인 술자리가 있으면 거절**(`ACTIVE_GATHERING_EXISTS`, "진행 중인 정산이 끝난 뒤 탈퇴할 수 있어요"). 없으면 개인정보 삭제. 완료된 술자리의 내 행은 7일 뒤 함께 사라지므로 익명화만 | iOS 심사 |

### 1. 인증 — P0

이미 있는 것: 카카오 로그인(웹 쿠키), `GET /auth/me`(`displayName`·`nickname`·`needsName`), `PUT /users/me/display-name`(FC-013).

**`/auth/me`에 더 필요한 것(2026-10-06 추가):** `spoonCount`(H1 내 캐릭터·칭호 칩), `payout`(§9, 본인 것), `unreadNotificationCount`(§12).

**1-1. 앱 로그인 (새로 필요, P0).** 앱은 카카오 SDK 없이 **서버 OAuth 를 앱 안 브라우저로** 연다
(Android Custom Tabs, iOS `ASWebAuthenticationSession`). 그런데 쿠키는 그 브라우저에 남지 앱으로 오지 않는다. 그래서:

```
GET  /api/v1/auth/kakao/login?client=app            → 카카오 인가 → 콜백
콜백 → 302  jeongsan://auth?ticket={1회용, 60초}     ← 앱이 이 스킴으로 돌아온다
POST /api/v1/auth/app/exchange  { "ticket": "…" }    → 200 { "token": "eyJ…", "expiresAt": "…" }
이후 모든 요청: Authorization: Bearer eyJ…            ← 서버는 쿠키 또는 Bearer 둘 다 받는다
```

- 티켓은 1회용·60초·서버 저장(토큰을 URL에 직접 싣지 않기 위해서다 — 다른 앱이 스킴을 가로챌 수 있다).
- 앱 스킴 `jeongsan://` 은 고정. 웹 흐름(쿠키)은 그대로 둔다.
- 401 `TOKEN_EXPIRED` 면 앱은 로그인 화면으로 보낸다(갱신 토큰 없음 — API.md §2.3 그대로).

**1-2. 웹 로그인 뒤 원래 링크로 (P0, FC-009 주의 사항).** `GET /auth/kakao/login?returnTo=/jungsan/j/{token}` —
콜백이 그 경로로 돌려보낸다. `returnTo`는 `/jungsan/`으로 시작하는 상대 경로만 허용(오픈 리다이렉트 방지).

**1-3. Apple 로그인 (D6, P0 if 결정).** 1-1과 같은 모양(`/auth/apple/login?client=app`). `users.provider = APPLE`.
Apple 은 이름을 첫 로그인에만 주고 안 줄 수도 있다 — 어차피 실명은 L2에서 받으므로(`needsName`) 문제없다.

**1-4. 탈퇴 (D7, P0).** `DELETE /api/v1/users/me` → 204. 거절은 409 `ACTIVE_GATHERING_EXISTS`.

**1-5. 로그아웃 (P1).** `POST /api/v1/auth/logout` — 쿠키 삭제. 앱은 토큰만 버리면 되지만 같은 호출을 보낸다.

### 2. 술자리 상세 — 모든 화면의 기본 응답 (P0)

`GET /api/v1/gatherings/{id}` → 200 `Gathering`. 참여자(ACTIVE)만 볼 수 있다. 아니면 403 `NOT_PARTICIPANT`.

```jsonc
{
  "id": 101, "title": "9/28 술자리", "date": "2026-09-28",
  "hostUserId": 1, "status": "OPEN",              // OPEN | SETTLING | COMPLETED
  "shareToken": "k7Qx2…", "inputRevision": 7, "completedAt": null,
  "participants": [
    { "id": 11, "userId": 1, "displayName": "김동규", "nickname": "동규짱", "spoonCount": 1280,
      "payout": { "bank": "카카오뱅크", "accountNo": "3333012345678", "holder": "김동규" } }  // 아래 공개 규칙
  ],
  "rounds": [
    { "id": 1, "seq": 1, "total": 184000, "payerParticipantId": 11,
      "drinks": [ { "name": "소주", "unitPrice": 5000, "quantity": 6 } ] }      // label 없음(D4) — 프론트가 "1차"
  ],
  "responses": [ { "participantId": 12, "roundId": 1, "type": "DRANK", "source": "SELF" } ],  // 빈칸은 행 없음
  "transfers": [                                                               // 정산 전 []
    { "id": 501, "fromParticipantId": 12, "toParticipantId": 11, "amount": 41000,
      "status": "WAITING", "sentAt": null, "confirmedAt": null, "notReceivedAt": null,
      "basis": [ { "roundId": 1, "type": "DRANK", "amount": 28700 } ] }        // D2
  ],
  "timeline": [
    { "id": 1, "type": "SYSTEM", "authorParticipantId": null, "body": "이민지님이 들어왔어요", "createdAt": "…" },
    { "id": 2, "type": "MESSAGE", "authorParticipantId": 13, "body": "2차는 불참!", "createdAt": "…" }
  ],
  "spoonGivers": [12],                    // 이 술자리에서 총무에게 스푼을 준 participantId
  "me": { "participantId": 11, "settlementViewed": true }    // 보는 사람 기준(D5). 프론트가 role 판단에 쓴다
}
```

- **payout 공개 규칙(계좌 보안, DOMAIN §9):** 참여자 X 의 `payout`은 **X 본인**이거나 **X 에게 보낼 송금이 있는 사람**에게만
  채운다. 그 밖엔 `null`. 대신 모든 참여자에게 `"hasPayout": true|false`를 준다 — "계좌 등록을 기다리는 중"과
  [계좌 등록] 배너 판단에 쓴다.
- **타임라인 `body`는 서버가 완성된 문장으로 준다.** 시스템 소식은 만든 때의 이름으로 굳는다(FC-013). 문구표는 §13.
- 이름은 `users`를 조인(FC-013). `nickname`이 없으면 `null`.
- 나간(REMOVED) 사람은 `participants`에 없다. 타임라인의 그 사람 글은 `authorParticipantId`가 남아도 된다 —
  프론트는 이름을 못 찾으면 "알 수 없음"으로 그린다.

### 3. 내 술자리 목록 (H1) — P0

`GET /api/v1/me/gatherings` → 200 `Gathering[]` (D1 = D 일 때). 내가 ACTIVE 참여자인 술자리 전부, `date` 내림차순.
7일 지나 삭제된 것은 없다. 탭 분류·뱃지·"지금 할 일"은 프론트가 한다.

### 4. 술자리 만들기·고치기 (총무) — P0

| 요청 | 규칙 |
|---|---|
| `POST /api/v1/gatherings` `{ title? }` → 201 `Gathering` | 제목은 주면 그 값(1~20자, FC-015 다음 차 새 술자리), 없으면 `M/d 술자리`, 날짜 오늘(KST), 만든 사람이 총무이자 첫 참여자, `shareToken` 발급, 타임라인 `CREATED` |
| `PATCH /gatherings/{id}` `{ title?, date? }` | 총무, `OPEN`만. 제목 1~20자 |
| `POST /gatherings/{id}/rounds` `{ total, payerParticipantId, drinks[] }` → 201 `Round` | 총무, `OPEN`. `seq` = 마지막+1. `total` ≥ 0, 결제자는 ACTIVE 참여자. 기존 참여자의 새 차수 칸은 **빈칸**(응답 행 없음) |
| `PUT /gatherings/{id}/rounds/{rid}` (같은 본문) | 총무, `OPEN` |
| `DELETE /gatherings/{id}/rounds/{rid}` | 총무, `OPEN`. 그 차수 응답도 삭제, 뒤 차수 `seq` 당김 |

모두 `input_revision` 올림. 차수 변경은 타임라인 소식 하나(§13).

### 5. 링크 참여 (P1 입구) — P0 · FC-009 그대로

- `GET /api/v1/join/{shareToken}` (로그인 없이) → `{ title, date, status, participantCount, host{displayName, spoonCount}, rounds[{id, seq, total}] }`
  - 없는 토큰 404 `GATHERING_NOT_FOUND`. 이름·응답·금액·계좌는 주지 않는다.
- `POST /api/v1/join/{shareToken}` `{ responses: [{roundId, type}] }` → 200 `{ gatheringId }`
  - 멱등(이미 참여 중이면 그대로 200), `status != OPEN` 이면 409 `GATHERING_NOT_OPEN`, `EXEMPT` 금지(400).
  - 실명 미등록(`needsName`)이면 409 `DISPLAY_NAME_REQUIRED` — 프론트는 이름 저장을 먼저 보낸다(FC-013).
  - 한 트랜잭션: 참여자 추가 → `SELF` 응답 → 타임라인 `JOINED` → `input_revision` 올림 → 총무 알림.
- 토큰 길이: API.md §3.4(12자·62종) 그대로 써도 된다. 프론트 공유 주소 `https://jungsan.devkdk.com/jungsan/j/{token}`(FC-012).

### 6. 응답 (P2) — P0

| 요청 | 규칙 |
|---|---|
| `PUT /gatherings/{id}/responses/me` `{ answers: [{roundId, type}] }` | 본인, `OPEN`. `SELF`로 저장. `EXEMPT`·`HOST`가 쓴 `EXEMPT` 칸은 건너뛴다(덮지 않음, 오류 아님) |
| `PUT /gatherings/{id}/participants/{pid}/responses` `{ answers }` | 총무 대리(R3). `source = HOST`. D3=A 면 참여자가 나중에 덮어쓸 수 있다 |

모두 `input_revision` 올림. 타임라인 "○○님이 응답했어요"(같은 사람이 연달아 고치면 하나로 합쳐도 된다).

### 7. 정산 (R3·P3) — P0

| 요청 | 규칙 |
|---|---|
| `GET /gatherings/{id}/settlement/preview` | 총무. FC-002 모양: `{ inputRevision, inputHash, lines[{participantId, total, auto, rounds[{roundId, type, amount}]}], transfers[{from, to, amount, basis[]}] }` |
| `POST /gatherings/{id}/settlement` `{ inputRevision, inputHash }` → 200 `Gathering` | 총무, `OPEN`. 불일치 409 `SETTLEMENT_INPUT_CHANGED`. 빈칸 → `AUTO`·`DRANK`. 송금·근거 스냅샷 저장(D2). `SETTLING`. 알림 FC-005 |
| `DELETE /gatherings/{id}/settlement` | 총무. **아무 송금도 `sentAt`이 없을 때만**(한 번이라도 [보냈어요]가 눌렸으면 409 `TRANSFER_ALREADY_SENT`). `OPEN`으로, 송금 삭제, `AUTO` 응답은 빈칸으로 되돌림 |
| `POST /gatherings/{id}/settlement/viewed` | 본인. `settlement_viewed_at` 기록(D5). 멱등 |

### 8. 송금 (P3·R1) — P0

| 요청 | 누가 | 바뀌는 것 |
|---|---|---|
| `POST /transfers/{tid}/sent` | 송금자 | `WAITING → SENT`, `sentAt`. 수취인 계좌가 없으면 409 `PAYOUT_MISSING` |
| `POST /transfers/{tid}/confirm` | 수취인 | `SENT`(또는 `WAITING`) `→ CONFIRMED`. 모든 송금 `CONFIRMED`면 `COMPLETED`, `completedAt`, 총무 스푼 +1(참여자 2명 이상) |
| `POST /transfers/{tid}/not-received` | 수취인 | `SENT → WAITING`, `notReceivedAt`. `sentAt`은 지우지 않는다(되돌리기 판단용) |

### 9. 계좌 (A1) — P0 · FC-011 그대로

`PUT /api/v1/users/me/payout` `{ bank, accountNo, holder }` → 200 `me`. 은행 목록·검증은 FC-011.
처음 등록이면 그 사람에게 보낼 미확인 송금이 있는 송금자에게 알림. `/auth/me`에 `payout`(본인 것, 마스킹 없이)도 준다.

### 10. 타임라인·스푼 — P1

- `POST /gatherings/{id}/messages` `{ text }` (1~200자, 앞뒤 공백 제거) → 201 `TimelineEntry`. 참여자만.
- `POST /gatherings/{id}/spoon` → 200. 총무가 아닌 참여자, `COMPLETED`만, 한 번만(두 번째는 멱등 200).
  총무 `users.spoon_count` +1, 타임라인 `SPOON`.
- **실시간(WebSocket)은 이번 출시에 없다.** 프론트는 화면에 들어올 때·앱이 앞으로 올 때·당겨서 새로고침 때 상세를 다시 부른다.

### 11. 참여자 관리 (R4) — P1 · FC-010 그대로

`DELETE /gatherings/{id}/participants/{pid}` — 거절 코드 `REMOVE_HOST`·`REMOVE_PAYER`·`GATHERING_NOT_OPEN`.
면제 API 는 보류(FC-010).

### 12. 알림 (N1) — P1 · FC-005 그대로

- `GET /api/v1/me/notifications` → `[{ id, type, gatheringId, title, body, createdAt, readAt }]` 최신순 50개.
- `GET /auth/me`에 `unreadNotificationCount`.
- `POST /me/notifications/{id}/read`, `POST /me/notifications/read-all`.
- `type` 값으로 프론트가 갈 곳을 고른다: `SETTLED`·`NOT_RECEIVED`·`PAYOUT_REGISTERED` → P3, `REMOVED` → H1, 나머지 → R1.
- 푸시(FCM·APNs)는 이번 출시에 없다(P2). 알림함만.

### 13. 타임라인 시스템 문구표 (서버가 `body`로 완성)

| 코드 | 문구 (지금 세 플랫폼 목데이터가 쓰는 문장 그대로) |
|---|---|
| `CREATED` | `{총무}님이 술자리를 만들었어요` |
| `JOINED` | `{이름}님이 들어왔어요` |
| `RESPONDED` | `{이름}님이 응답했어요` / 두 번째부터 `{이름}님이 응답을 고쳤어요` |
| `ROUND_SAVED` | `{seq}차 {total}원을 넣었어요` / 고치면 `…원을 고쳤어요` (금액은 `184,000` 처럼 쉼표) |
| `ROUND_DELETED` | `{seq}차를 지웠어요` |
| `PAYOUT_SAVED` | `{이름}님이 받을 계좌를 등록했어요` / 두 번째부터 `…바꿨어요` (계좌번호는 넣지 않는다) |
| `SETTLED` | `{총무}님이 정산했어요` · 자동응답이 있으면 뒤에 ` · {이름·이름}님은 응답이 없어 전 차수 참석·알코올로 계산됐어요` |
| `SETTLEMENT_REVERTED` | `{총무}님이 정산을 되돌렸어요` |
| `SENT` | `{송금자}님이 보냈어요` |
| `CONFIRMED` | `{수취인}님이 {송금자}님 입금을 확인했어요` |
| `NOT_RECEIVED` | `{수취인}님이 아직 {송금자}님 입금을 확인 못 했어요` |
| `COMPLETED` | `모두 입금 완료! 🎉` |
| `REMOVED` | `{이름}님이 빠졌어요` |
| `SPOON` | `{이름}님이 총무에게 한 스푼 줬어요` |

이름은 실명(`displayName`)만, 닉네임은 붙이지 않는다(FC-013).

### 14. 우선순위 요약 (RELEASE 작업표와 맞춤)

| 날짜 | 서버가 먼저 열어 주면 프론트가 바로 붙이는 것 |
|---|---|
| 10/5 | 1-1 앱 로그인, 1-2 returnTo, 2 상세, 3 목록, 4 만들기·차수, 5 참여, 6 응답 |
| 10/6 | 7 정산, 8 송금, 9 계좌, 1-3 Apple(D6), 1-4 탈퇴(D7) |
| 10/7 | 10 타임라인·스푼, 11 내보내기, 12 알림 |

- **반영할 곳:** `API.md`(v3 절 전면 개정 — 모임 절 삭제), `DOMAIN_DB_DESIGN_V2.md`(`settlement_viewed_at`, 송금 근거 테이블,
  앱 로그인 티켓), `REQUIREMENTS.md` §9(탈퇴)·§10(알림 표)
