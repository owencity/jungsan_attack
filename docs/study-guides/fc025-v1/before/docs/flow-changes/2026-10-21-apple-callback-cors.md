# 2026-10-21 — Apple 로그인 콜백이 CORS로 막힌다 (운영 장애)

## FC-025 Apple `form_post` 콜백이 403 "Invalid CORS request"

- **상태:** 열림 (긴급 — App Store 심사관은 Apple 로그인으로만 들어온다)
- **바뀐 흐름:** 없음. 서버 버그다. 웹(2026-10-21 Apple 로그인 버튼 추가, 웹 `87be8e6`)과 앱(브라우저 + PKCE, FC-021)이
  **같은 콜백** `POST /api/v1/auth/apple/callback`을 쓴다. 웹에서 Apple로 로그인하면 "Invalid CORS request"만 보이고 끝난다.
  앱의 Apple 로그인도 같은 콜백이라 같은 이유로 실패한다고 본다(카카오는 GET 리다이렉트라 영향 없음).
- **원인:** `AuthPolicy`가 Apple 인가 요청에 `response_mode=form_post`를 붙인다 → Apple이 `appleid.apple.com` 페이지에서 우리
  콜백으로 **폼 POST**를 한다 → 브라우저가 `Origin: https://appleid.apple.com`을 붙인다 → `WebConfig.addCorsMappings`가
  `/api/**`에 `app.frontend-origin`(jungsan.devkdk.com) 하나만 허용해서 Spring CORS 처리기가 403으로 거절한다.
- **재현(운영, 2026-10-21):**
  ```
  curl -i -X POST https://jungsan-api.devkdk.com/api/v1/auth/apple/callback \
    -H "Origin: https://appleid.apple.com" -H "Content-Type: application/x-www-form-urlencoded" --data "code=x&state=y"
  → HTTP/1.1 403 Forbidden · 본문 "Invalid CORS request"
  ```
- **백엔드 영향(제안):**
  1. `WebConfig`에서 `/api/v1/auth/apple/callback`만 `https://appleid.apple.com` 출처의 `POST`를 허용한다. 더 구체적인 매핑을
     `/api/**`보다 **먼저** 등록한다(첫 일치가 쓰인다). 다른 API의 CORS 허용 범위는 넓히지 않는다
  2. 상태 쿠키는 이미 Apple일 때 `SameSite=None; Secure`다(`AuthController`) — 크로스 사이트 POST에도 실리는지 같이 확인
  3. 테스트: `Origin: https://appleid.apple.com` POST 콜백이 403이 아니라 기존 처리(정상 state면 302)로 들어가는지, 다른 경로에
     같은 Origin을 보내면 여전히 403인지
- **확인 방법(배포 뒤):** 위 curl이 403이 아니라 state 불일치 처리(로그인 실패로 302)로 바뀐다. 이어서 실제 Apple 계정으로
  웹 `https://jungsan.devkdk.com/jungsan` → [Apple로 계속하기] → 이름 화면까지, TestFlight 빌드 8 → [Apple로 계속하기] → 이름 화면까지
- **기존 문서와 충돌:** 없음. FC-021·FC-022의 Apple 로그인 운영 확인에서 콜백 POST가 빠져 있었다
- **반영할 곳:** `WebConfig.kt`, Apple 콜백 HTTP 테스트, FC-021 운영 확인 기록
