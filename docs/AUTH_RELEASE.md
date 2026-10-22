# 출시 인증·탈퇴 계약 (2026-10-09)

CTO가 앱 Bearer / 웹 httpOnly 쿠키를 결정했다. 기존 쿠키 전용 규칙을 이 범위에서 대체한다.
JWT 만료 14일/30일 미결은 변경하지 않는다. 실제 만료는 설정값을 응답한다.

## 로그인

- **FC-025 CORS:** `/api/v1/auth/apple/callback`만 `Origin: https://appleid.apple.com`의 POST와 credentials를 허용한다. 구체 경로를 `/api/**`보다 먼저 등록하며 다른 API는 기존 프론트 출처만 허용한다. 상태·브라우저 상관 쿠키·nonce 검증은 그대로다. 잘못된 `state=y`는 401 `UNAUTHENTICATED`다. 정상 완료는 FC-026에 따라 웹303·앱200 HTML이다.
- Apple 상태 쿠키는 운영에서 `HttpOnly; Secure; SameSite=None; Path=/api/v1/auth; Max-Age=300`이다. WEB JWT는 `SameSite=Lax`를 유지한다. 발급 속성과 서버의 쿠키 수신 테스트는 실제 Safari·TestFlight 브라우저 전달·실계정 로그인 완료와 구분한다.

- `GET /api/v1/auth/{kakao|apple}/login?client=web|app&returnTo=/jungsan/...`
  - 웹 기본값. 상대 경로 `/jungsan/`만 허용하며 `//`, 역슬래시, 제어문자, percent 인코딩을 거절한다.
  - 앱은 추가로 `codeChallenge` (SHA-256, base64url 43자)를 보낸다. 앱이 생성한 43~128자 `codeVerifier`는 앱에만 보관한다. [RFC 7636 §4.1·4.2](https://www.rfc-editor.org/rfc/rfc7636#section-4.1)의 문자·길이·S256 방식을 티켓 교환에 적용한다.
  - 5분짜리 일회용 state·nonce를 DB에 저장한다. 브라우저 httpOnly 상관 쿠키와 state를 함께 검증한다.
  - Apple은 `response_mode=form_post`를 유지하고 `scope`를 보내지 않는다. 이름·이메일 제공 동의를 요청하지 않는다. HTTPS 상관 쿠키는 SameSite=None이다.
- 카카오 콜백 GET / Apple 콜백 POST(form-urlencoded). 코드 교환 뒤 Apple ID 토큰의 RS256 서명·kid·issuer·audience·exp·nonce·subject를 검증한다. 계정 연결에는 `sub`만 사용하며 Apple 이름·이메일은 사용하거나 저장하지 않는다. 실명은 L2 등록 화면에서 따로 받는다. 이전 동의로 ID 토큰에 이메일이 포함되더라도 무시한다([Apple 인가 문서](https://developer.apple.com/documentation/signinwithapplerestapi/request-an-authorization-to-the-sign-in-with-apple-server)).
- 웹: `jeongsan_token` 쿠키 발급, 원래 경로로303 복귀. 앱: 아래 완료 페이지를 통해 고정 `jeongsan://auth?ticket=...`로 복귀한다. JWT는 URL에 넣지 않는다.
- `POST /api/v1/auth/app/exchange {ticket,codeVerifier}` → `{token,expiresAt}`. **5분 티켓**은 DB 잠금·삭제로 한 번만 교환되고 앱의 challenge와 대조한다. 만료 시각부터 거절한다. 기존 적용 changelog017의60초 설명은 과거 값이며 스키마 변경 없이 현재 AuthPolicy의300초를 적용한다.
- 앱 Authorization Bearer는 APP 토큰만 받는다. 쿠키는 WEB 토큰만 받는다. 둘 다 있으면 Bearer를 우선하고 잘못된 헤더를 쿠키로 우회하지 않는다.
- `POST /api/v1/auth/logout` → 204 + 쿠키 만료. 사용한 JWT의 해시를 만료까지 저장해 복사된 토큰도 무효화한다.

### FC-026 앱 로그인 완료 페이지

- 카카오 GET·Apple POST 콜백이 앱 로그인에 성공하면 **200 `text/html; charset=UTF-8`** 완료 페이지를 반환한다. 직접303 외부 앱 전환 대신 자동 전환을 한 번 시도하고, 브라우저가 막아도 `[정산어택 앱으로 돌아가기]` 링크가 남는다.
- UA에 Android가 있으면 `intent://auth?ticket=...#Intent;scheme=jeongsan;package=app.jeongsan;end`, 그 밖에는 `jeongsan://auth?ticket=...`다. User-Agent는 UI 선택에만 사용하고 HTML에 그대로 반영하거나 인증 판단에 사용하지 않는다. 앱의 기존 스킴·패키지 계약을 유지하며 재빌드가 필요하지 않다.
- 티켓은 서버가 생성한43자 base64url만 링크 속성에 허용한다. 외부 스크립트·이미지·폰트·통신을 사용하지 않는다. `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, `X-Content-Type-Options: nosniff`, 외부 자원·프레임·폼을 차단하는 CSP와 고정 스크립트 SHA256 허용 목록을 적용한다. 티켓·코드·state를 서버 로그에 넣지 않는다.
- 고정 스크립트가 먼저 `history.replaceState`로 주소의 인증 query를 제거한 뒤 링크 이동을 시도한다. 수동 링크는 JavaScript가 없어도 동작한다. 자동 전환 성공을 보장하지 않는다.
- Apple POST의 직접200 응답은 문서의 A안을 따른다. 새로고침으로 이미 소비한 code/state가 재전송되면 기존401이고 새 티켓을 발급하지 않는다. 만료·재전송 실패 시 앱에서 로그인을 다시 시작한다. 별도 중계 URL/저장소·App Links/Universal Links는 추가하지 않는다.
- 웹은 계속303·WEB 쿠키·원래 복귀 경로를 사용한다. 앱에는 WEB JWT 쿠키를 발급하지 않고 상관 쿠키만 만료한다. 실제 Android 카카오톡 왕복과 수동 버튼 복귀는 운영 반영 뒤 별도 확인한다.

### 로그아웃 백엔드 연결 계약 (2026-10-22)

- API는 이미 운영에 존재한다. 본문 없이 `POST /api/v1/auth/logout`, 앱은 `Authorization: Bearer <APP 토큰>`, 웹은 기존 httpOnly WEB 쿠키를 보낸다.
- 성공은 **204·빈 본문**, `Cache-Control: no-store`, WEB 쿠키 만료(`Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax`, Secure는 운영 기준)다. 앱은 JSON 응답을 읽지 않는다.
- 전달한 **현재 자격만** 무효화한다. 같은 계정의 다른 기기 WEB/APP 토큰은 유지한다. 카카오 계정 자체의 브라우저 세션 종료·연결 해제·회원 탈퇴와는 별도다.
- 이후 같은 토큰의 보호 API·로그아웃 재호출은 기존 **401 `UNAUTHENTICATED`**다. 동시 요청은 인증 검사 시점에 따라204 또는401이며 무효화 행은 한 개다. 반복 요청을 새204 계약으로 바꾸지 않는다.
- 프론트는204 또는 이미 무효화된401 후 로컬 토큰·로그인 상태를 제거한다. 네트워크 실패 때 로컬 토큰만 버린 경우 서버 무효화 성공으로 보고하지 않는다. 이번 작업에서 프론트 코드는 변경하지 않았다.

### 로그인 복귀 진단

- `AUTH_LOGIN_STARTED provider=KAKAO client=app`: 서버가 인가 시작을 처리했다.
- `AUTH_CALLBACK_FAILED provider=KAKAO reason=...`: INVALID_STATE, INVALID_CODE, MISSING_BROWSER_COOKIE, STATE_NOT_FOUND, STATE_EXPIRED, PROVIDER_MISMATCH, BROWSER_MISMATCH, PROVIDER_REJECTED 중 하나다. 공개401/503 등 기존 응답은 유지한다.
- `AUTH_CALLBACK_COMPLETED provider=KAKAO client=app`: 트랜잭션이 커밋돼 티켓과 복귀 응답을 구성했다. **브라우저가 실제 앱을 열었거나 티켓 교환에 성공했다는 뜻은 아니다.**
- 로그에는 code·state/해시·쿠키·nonce·티켓·JWT·사용자 ID·이름·예외 본문·요청 URL을 기록하지 않는다. 제공자/client는 허용 목록으로 제한한다. 요청별 추적 ID나 전체 접근 로그를 추가하지 않는다.
- Android 복귀 흐름은 [FC-026](flow-changes/2026-10-22-app-return-page.md), 초기 조사와 로그아웃 기록은 [운영 조사](operations/android-auth-investigation.md)에 보존한다.

## 탈퇴

- `DELETE /api/v1/users/me` → 204 + 쿠키 만료.
- 참여하거나 생성한 술자리 중 OPEN·SETTLING이 있으면 409 `ACTIVE_GATHERING_EXISTS` (FC-014 D7 결정).
- 완료 자료는 금액·좌석 ID를 유지하되 사용자 연결·이름·계좌를 익명화한다. 생성한 술자리 제목도 기본 제목으로 바꾼다. 로그인 사용자 행, 본인 메시지·알림·로그인 티켓을 삭제한다. 완료 술자리는 기존 7일 삭제를 따른다.
- 완료 응답의 탈퇴 좌석 `userId`·`nickname`과 생성 계정 `createdByUserId`는 null이다. 좌석 ID·단위 총무 ID·확정 송금은 유지되고 표시 이름은 `탈퇴한 사용자`, 스푼은 0이다.
- Apple refresh token은 암호화 저장한다. 탈퇴 트랜잭션에서 암호문만 연결 해제 작업에 넘기고 계정 개인정보를 삭제한다. 별도 재시도 작업이 Apple `/auth/revoke`를 호출하고 성공 후 작업 행도 삭제한다. 계정 삭제와 외부 HTTP를 같은 트랜잭션으로 묶지 않는다.
- 삭제는 관련 단위를 ID순 → 술자리를 ID순 → 사용자 순으로 잠근다. 사용자 잠금 후 관련 목록을 다시 검사하며 바뀌면 409 `ACCOUNT_STATE_CHANGED`, 재시도한다. 새 술자리 생성은 사용자 공유 잠금, 참여·추가 단위 생성은 술자리 잠금 후 사용자를 재검사한다.
- 로그인 토큰 검증 시 사용자 존재·로그아웃 해시를 확인하므로 탈퇴 전 JWT는 바로 401이다.

## 검증·운영

Apple 키·ID는 환경설정만 참조한다. 2026-10-10 CTO 결정으로 로컬·운영 모두 Apple 설정이 비면 서버는 기동하고 Apple 인가·코드 교환·연결 해제만 503 `AUTH_PROVIDER_UNAVAILABLE`로 닫는다. 카카오와 기존 기능의 인증은 그대로 적용한다.
운영 `.env`에는 Apple 설정 키 이름을 모두 선언하고 발급 전 값은 빈칸으로 둔다. placeholder를 실제 설정으로 취급하지 않는다. 설정을 모두 채웠다면 잘못된 EC 개인키는 기동 시 거절한다. 카카오·DB·JWT·독립 암호화 키 등 나머지 필수 설정은 계속 즉시 실패한다. 기존 의존성과 JDK/JJWT만 사용한다.

이 구현과 앱의 실제 연결, 운영 제공자 로그인 시험, 심사 제출·승인은 별도 상태다. 진행 중 술자리 탈퇴 거절이 심사에서 수용되는지는 단정하지 않는다.

근거: [Apple 인증](https://developer.apple.com/documentation/signinwithapple/authenticating-users-with-sign-in-with-apple),
[Apple 토큰 검증](https://developer.apple.com/documentation/signinwithapple/verifying-a-user),
[탈퇴 안내](https://developer.apple.com/support/offering-account-deletion-in-your-app/),
[연결 해제](https://developer.apple.com/documentation/signinwithapplerestapi/revoke-tokens).
