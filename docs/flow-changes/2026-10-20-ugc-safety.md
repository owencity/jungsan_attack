# 2026-10-20 — 정산방 메시지 신고·차단 (App Store 1.2 사용자 생성 콘텐츠)

## FC-024 메시지 신고·차단·금칙어·이용약관 동의

- **상태:** 열림 (CTO 결정 2026-10-20 — 메시지 기능을 유지하고 신고·차단을 만든다(B안). 알림 ⓐ·자동 숨김 2명 결정)
- **배경:** App Store 첫 심사에서 Apple이 "사용자 생성 콘텐츠가 있으면 **필수인 신고·차단 기능**을 녹화로 보여 달라"고
  요청했다(Guideline 2.1 정보 요청, 1.2 근거). 정산방 "한마디 남기기"(`POST /gatherings/{gid}/messages`, 타임라인 `MESSAGE`)가
  사용자 생성 콘텐츠다. 지금은 신고·차단이 없어 이대로면 1.2로 반려된다.
  Apple 1.2가 요구하는 것: ① 부적절한 내용이 올라가지 않게 거르는 방법 ② 신고 수단과 **신속한 대응**(관행상 24시간)
  ③ 악성 사용자 **차단** ④ 공개된 연락처(지원 페이지에 이미 있음) + 이용약관에 "부적절한 콘텐츠 무관용" 동의.

### 바뀐 흐름 (웹·Android·iOS 같음)

```text
정산방 타임라인의 남의 메시지 → 길게 누르기(웹: 메시지 옆 ⋯)
  → 시트: [신고하기] [○○님 차단하기]
  → 신고하기 → 사유 하나 고르기(욕설·비방 / 음란·불쾌 / 스팸·광고 / 기타) → [신고]
     → 그 메시지는 나에게서 바로 사라짐 · 토스트 "신고했어요. 24시간 안에 확인할게요"
  → 차단하기 → 확인 → 그 사람의 메시지가 모든 술자리에서 나에게 안 보임(정산·명단·금액은 그대로)
내 계정 → [차단한 사람] → 목록 · [차단 풀기]
첫 로그인 L2(이름 확인)·링크 참여 P1(첫 로그인) → [이용약관에 동의합니다] 체크해야 시작 버튼이 켜짐
```

- **정산에는 영향이 없다.** 차단해도 같은 술자리 명단·차수·송금은 그대로다(돈 계산을 깨지 않는다). 메시지만 숨긴다.
- 내 메시지·시스템 소식(SYSTEM·SPOON)에는 신고·차단 메뉴가 없다.

### 백엔드 영향 (제안 — 이름·경로는 백엔드가 정해도 된다)

1. **신고** — `POST /api/v1/gatherings/{gid}/messages/{entryId}/reports` `{reason}` → 204
   - `reason`: `ABUSE`(욕설·비방) · `SEXUAL`(음란·불쾌) · `SPAM`(스팸·광고) · `OTHER`
   - 그 술자리 참여자만. `MESSAGE`만(SYSTEM·SPOON은 400). 내 메시지 신고는 400. 같은 사람이 같은 메시지를 다시 신고하면 204(멱등)
   - 저장: `message_reports(id, entry_id, reporter_user_id, reason, created_at, handled_at)`
   - **신고한 사람에게는 그 메시지를 바로 숨긴다** — `GET /gatherings/{id}`의 `timeline`에서 내가 신고한 항목을 뺀다
   - **서로 다른 2명이 신고하면 모두에게서 자동으로 숨긴다**(`timeline_entries.hidden_at`) — 운영자가 보기 전에도 퍼지지 않게
   - **운영자 알림:** 서버 로그 + 하루 한 번 미처리 신고 조회(결정 ⓐ, 아래)
2. **차단** — 사용자 단위
   - `POST /api/v1/me/blocks` `{gatheringId, participantId}` → 204 — 서버가 참여자 → 사용자로 바꿔 저장(프론트는 user id를 모른다)
   - `GET /api/v1/me/blocks` → `[{userId, displayName, blockedAt}]` · `DELETE /api/v1/me/blocks/{userId}` → 204
   - 나 자신 차단은 400. 이미 차단이면 204(멱등)
   - 효과: **내가 받는 모든 `timeline`에서 차단한 사용자가 쓴 `MESSAGE`를 뺀다.** 상대에게는 알리지 않는다. 명단·응답·송금은 그대로
   - 저장: `user_blocks(blocker_user_id, blocked_user_id, created_at)` PK 두 열
3. **금칙어** — `POST /gatherings/{gid}/messages`에서 금칙어(욕설·성적 표현 기본 목록, 서버 리소스 파일)가 있으면
   422 `MESSAGE_NOT_ALLOWED` "부적절한 표현이 있어요" → 저장하지 않는다. 띄어쓰기·특수문자 끼워 넣기 정도는 정규화 후 비교
4. **운영 조치** — 관리자 화면은 만들지 않는다. 운영 문서에 SQL 절차: 메시지 숨기기(`hidden_at`), 상습 사용자 메시지 금지
   (`users.message_banned_at` → 그 사람의 `POST messages`는 403 `MESSAGE_BANNED`), 처리 완료(`handled_at`)
5. **이용약관 동의 기록(선택)** — 프론트 체크로 Apple 요구는 충족한다. 서버가 기록하려면 실명 등록 요청에 `termsVersion`을 받아
   `users.terms_agreed_at·terms_version`에 남긴다. 안 해도 프론트는 동작한다

### 프론트 (이번에 같이 만든다)

- 웹 `v3/RoomPage.tsx`·앱 `RoomScreen.kt` 타임라인 메시지 메뉴, 신고 사유 시트, 차단 확인, 계정 화면 [차단한 사람]
- 목데이터 모드는 화면 안에서 숨김만 흉내 낸다(`store.ts`·`V3Store.kt`의 `reportedEntryIds`·`blockedParticipantIds`)
- API 모드: 신고·차단 성공 뒤 술자리를 다시 읽어 서버가 뺀 타임라인을 그대로 그린다. 서버가 아직 없으면(404) "잠시 뒤 다시 시도해주세요"
- 이용약관 페이지 `/jungsan/terms`(로그인 없이 열림) — 무관용 조항, 신고 24시간 처리, 차단, 연락처
- 금칙어 422는 서버 `message`를 그대로 토스트로 보여준다

### 기존 문서와 충돌

- `API.md` 메시지 절(v6 표 `POST /gatherings/{gid}/messages`)에 신고·차단·금칙어가 없다. `DOMAIN_DB_DESIGN_V2.md`에 테이블 3개(+열 2개) 추가
- REQUIREMENTS 타임라인 절에 "신고·차단·무관용 약관"을 더해야 한다

### 결정 (CTO 2026-10-20)

- **신고 운영자 알림: ⓐ 서버 로그 + 하루 한 번 확인.** 신고마다 찾기 쉬운 한 줄 로그(`MESSAGE_REPORTED entryId=… gatheringId=… reason=…`,
  본문·계좌 등 개인정보 없이)를 남기고, 운영자는 하루 한 번 미처리 신고(`handled_at IS NULL`)를 조회한다. 운영 문서에 조회 SQL을 둔다.
  하루 안에 처리하므로 약관의 "24시간 안에 확인"과 맞는다. 즉시 알림(n8n)은 나중에 필요하면 붙인다
- **자동 숨김: 서로 다른 신고자 2명이면 모두에게서 숨긴다**(`hidden_at`). 1명 신고는 그 신고자에게만 숨긴다

### 반영할 곳

`API.md`(신고·차단·금칙어), `DOMAIN_DB_DESIGN_V2.md`(message_reports·user_blocks·hidden_at·message_banned_at), 운영 문서(신고 처리 SQL),
REQUIREMENTS 타임라인 절. **App Store 재심사 녹화가 이 기능을 기다린다 — 출시 경로의 가장 앞이다.**
