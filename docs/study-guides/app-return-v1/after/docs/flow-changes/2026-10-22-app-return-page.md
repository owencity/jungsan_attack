# 2026-10-22 — 앱 로그인 뒤 앱으로 안 돌아온다 (안드로이드, 카카오톡 로그인)

## FC-026 로그인 완료 페이지 + [앱으로 돌아가기] — 커스텀 스킴 자동 이동이 막힌다

- **상태:** 열림 (CTO 제보 2026-10-22, 안드로이드 실기기)
- **증상:** 안드로이드 앱 → [카카오로 시작] → 크롬의 카카오 로그인 → [카카오톡으로 로그인] → 카카오톡 앱에서 동의 →
  크롬으로 돌아옴 → **검은(빈) 화면에서 멈추고 앱으로 안 돌아온다.** 다시 시도하니 로그인은 됐다.
- **원인(코드 확인, 기기 재현 전):** `AuthController.finish`가 앱 로그인이면 `303 Location: jeongsan://auth?ticket=…`로 바로 보낸다.
  크롬은 **사용자 동작 없이 리다이렉트로 외부 앱(커스텀 스킴)을 여는 것을 막는다** — 특히 다른 앱(카카오톡)에 다녀온 뒤의
  리다이렉트 체인. 막히면 안내 없이 빈 화면이 남는다. 앱으로 넘기는 티켓은 60초짜리라(`AuthFlowService`, auth_tickets) 멈춘 채
  시간이 지나면 다시 해야 한다. iOS 사파리는 보통 "열겠습니까?"를 묻지만 같은 구조라 안전하다고 단정하지 않는다.
- **바뀐 흐름(제안 A — 지금):**
  ```text
  로그인 콜백(카카오 GET·Apple POST) → 앱 로그인이면 303 대신 **200 HTML "로그인 완료" 페이지**
    · 큰 버튼 [정산어택 앱으로 돌아가기]
      - 안드로이드(UA에 Android): href = intent://auth?ticket=…#Intent;scheme=jeongsan;package=app.jeongsan;end
      - 그 밖(iOS 등): href = jeongsan://auth?ticket=…
    · 페이지가 열리자마자 한 번 자동 시도(location 이동) — 허용되는 브라우저에선 바로 넘어가고, 막히면 버튼이 남는다
    · 안내: "앱이 열리지 않으면 버튼을 눌러주세요. 5분 안에 눌러야 해요"
  ```
  웹 로그인(client=web)은 지금처럼 303으로 웹에 돌아간다(바꾸지 않는다).
- **백엔드 영향:**
  1. `finish`의 앱 분기를 HTML 응답으로. `Cache-Control: no-store`, `Referrer-Policy: no-referrer` 유지, 티켓을 페이지 밖으로
     흘리지 않는다(외부 스크립트·이미지 없음, 인라인 스타일만). CSP `default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'` 정도
  2. 티켓 유효 시간 60초 → **5분**(버튼을 누를 여유). 한 번 쓰면 지우는 규칙·PKCE 검증은 그대로
  3. 테스트: 앱 콜백이 200 HTML이고 안드로이드 UA면 `intent://…;package=app.jeongsan;end`, iOS UA면 `jeongsan://auth?ticket=`가 들어 있는지.
     웹 콜백은 여전히 303
- **프론트 영향:** 없음. 앱은 지금처럼 `jeongsan://auth?ticket=`를 받는다(안드로이드 intent:// 도 같은 스킴으로 들어온다 —
  `AndroidManifest`의 `jeongsan://auth` 인텐트 필터, `MainActivity.onNewIntent`).
- **나중에(제안 B — 출시 뒤):** App Links/Universal Links — `https://jungsan.devkdk.com/app/auth?ticket=…`를 앱이 직접 받게 등록
  (웹 `/.well-known/assetlinks.json`·`apple-app-site-association`, 앱 매니페스트 autoVerify·Associated Domains). 리다이렉트도 막히지 않는다.
  Play 앱 서명 인증서 SHA-256이 필요하다. 웹·앱·서버 모두 바뀌어 지금은 하지 않는다
- **기존 문서와 충돌:** FC-021(앱 로그인 연결)의 "브라우저 → 303 jeongsan://" 복귀 방식이 바뀐다
- **반영할 곳:** `AuthController.finish`, `AuthFlowService` 티켓 TTL, API.md 앱 로그인 절, FC-021 기록

### 백엔드 구현·검증 후속 (PR #47)

- 프론트 문서 `67d2b7c`와 CTO의 문서 확인 요청을 반영해 PR #47을 진단·로그아웃 보완에서 실제 A안 구현으로 확장했다. 초기 백엔드가 별도로 붙인026 조사 문서는 [운영 조사](../operations/android-auth-investigation.md)로 이동해 번호 중복을 해소했다.
- 앱 콜백200 HTML·Android Intent/다른 환경 기존 스킴·자동1회 시도와 수동 링크·티켓300초를 구현한다. Java/Kotlin 링크·만료 정책은 독립 구현, 렌더링·HTTP는 공유한다. WEB303·Cookie/PKCE/state/nonce/일회용 검증·기존 Apple CORS는 유지한다.
- CSP는 제안의 unsafe-inline 스크립트 대신 고정 코드 SHA256으로 제한한다. URL의 인증 query를 history.replaceState로 지우고 외부 자원·프레임·폼을 차단한다. 티켓·UA를 임의 HTML/스크립트에 주입하지 않는다.
- Apple POST 직접200의 새로고침은 소비된state의 기존401로 처리한다. 원래 문서가 제안한 A안 범위를 유지하고 별도 완료 URL·임시 저장소를 추가하지 않는다. UA가 Android를 숨기면 기존 스킴의 수동 링크를 제공한다.
- 상태는 운영 반영·Android 실기기 왕복 확인까지 열림이다. 코드/단위/MVC·실제DB·CI·병합/배포 결과는 PR에 구분해 기록한다. Chrome 차단 설명은 코드·공식 정책에 근거한 원인 후보이며 이 환경에서 실기기 재현을 완료했다고 보고하지 않는다.
