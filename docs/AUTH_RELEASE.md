# 출시 인증·탈퇴 계약 (2026-10-09)

CTO가 앱 Bearer / 웹 httpOnly 쿠키를 결정했다. 기존 쿠키 전용 규칙을 이 범위에서 대체한다.
JWT 만료 14일/30일 미결은 변경하지 않는다. 실제 만료는 설정값을 응답한다.

## 로그인

- `GET /api/v1/auth/{kakao|apple}/login?client=web|app&returnTo=/jungsan/...`
  - 웹 기본값. 상대 경로 `/jungsan/`만 허용하며 `//`, 역슬래시, 제어문자, percent 인코딩을 거절한다.
  - 앱은 추가로 `codeChallenge` (SHA-256, base64url 43자)를 보낸다. 앱이 생성한 43~128자 `codeVerifier`는 앱에만 보관한다. [RFC 7636 §4.1·4.2](https://www.rfc-editor.org/rfc/rfc7636#section-4.1)의 문자·길이·S256 방식을 티켓 교환에 적용한다.
  - 5분짜리 일회용 state·nonce를 DB에 저장한다. 브라우저 httpOnly 상관 쿠키와 state를 함께 검증한다.
  - Apple은 `response_mode=form_post`를 유지하고 `scope`를 보내지 않는다. 이름·이메일 제공 동의를 요청하지 않는다. HTTPS 상관 쿠키는 SameSite=None이다.
- 카카오 콜백 GET / Apple 콜백 POST(form-urlencoded). 코드 교환 뒤 Apple ID 토큰의 RS256 서명·kid·issuer·audience·exp·nonce·subject를 검증한다. 계정 연결에는 `sub`만 사용하며 Apple 이름·이메일은 사용하거나 저장하지 않는다. 실명은 L2 등록 화면에서 따로 받는다. 이전 동의로 ID 토큰에 이메일이 포함되더라도 무시한다([Apple 인가 문서](https://developer.apple.com/documentation/signinwithapplerestapi/request-an-authorization-to-the-sign-in-with-apple-server)).
- 웹: `jeongsan_token` 쿠키 발급, 원래 경로로 복귀. 앱: 고정 `jeongsan://auth?ticket=...`로 복귀. JWT는 URL에 넣지 않는다.
- `POST /api/v1/auth/app/exchange {ticket,codeVerifier}` → `{token,expiresAt}`. 60초 티켓은 DB 잠금·삭제로 한 번만 교환되고 앱의 challenge와 대조한다.
- 앱 Authorization Bearer는 APP 토큰만 받는다. 쿠키는 WEB 토큰만 받는다. 둘 다 있으면 Bearer를 우선하고 잘못된 헤더를 쿠키로 우회하지 않는다.
- `POST /api/v1/auth/logout` → 204 + 쿠키 만료. 사용한 JWT의 해시를 만료까지 저장해 복사된 토큰도 무효화한다.

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
