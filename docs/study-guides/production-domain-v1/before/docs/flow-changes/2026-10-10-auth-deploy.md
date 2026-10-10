# 2026-10-10 — 앱 로그인 프론트 연결과 운영 배포 확인

## FC-021 앱 로그인 연결 완료 · 운영 서버가 아직 안 떠 있음 · Apple 콜백 도메인 불일치

- **상태:** 열림 (DEPLOY.md 도메인·설정 절차 수정, 운영·콘솔·연결 시험 후속)
- **바뀐 흐름:** 앱·웹 프론트가 `feat/auth-release` 계약에 붙었다(앱 084ce79 · 웹 eac5f99).
  - 앱: 브라우저로 `/auth/{kakao|apple}/login?client=app&codeChallenge=BASE64URL(SHA256(verifier))` →
    `jeongsan://auth?ticket=…` 복귀(Android intent-filter·iOS URL scheme) → `POST /auth/app/exchange {ticket, codeVerifier}` → Bearer 저장.
    Apple 버튼은 iOS 에만
  - 앱·웹: 내 술자리 → 계좌 화면 맨 아래 [로그아웃](`POST /auth/logout`) · [회원 탈퇴](`DELETE /users/me`, 두 번 눌러야)
- **백엔드·인프라 영향 — 실제 로그인이 막혀 있는 곳:**
  1. **운영 API 가 응답하지 않는다** — 2026-10-09 23시 `https://api.devkdk.com/api/v1/auth/me` → **502**(Cloudflare 는 살아 있고 원 서버가 없음).
     `feat/auth-release`를 OCI 에 배포해야 한다
  2. **`docs/DEPLOY.md` 콜백 도메인이 둘로 갈린다** — `KAKAO_REDIRECT_URI`는 `api.devkdk.com`, `APPLE_REDIRECT_URI`는
     `api.jungsan.devkdk.com`. 후자는 DNS 가 없다(접속 불가). 한쪽으로 맞추고 Apple Services ID Return URL 도 같은 값으로
  3. 카카오 콘솔 운영 Redirect URI 등록(`DEPLOY.md` (d))
  4. 웹 운영(Vercel)에 `VITE_JEONGSAN_API_BASE_URL` 을 넣어야 웹이 목데이터에서 서버로 바뀐다 — 쿠키는 `jungsan.devkdk.com` ↔
     `api.devkdk.com` 이 같은 사이트(devkdk.com)라 `SameSite=Lax`로 전달된다. CORS `FRONTEND_ORIGIN`만 맞으면 된다
- **확인 못 한 것:** 실제 카카오·Apple 로그인 → 티켓 교환. 운영 서버가 뜨면 앱(TestFlight)·웹에서 한 번 끝까지 돌린다
- **반영할 곳:** `DEPLOY.md` 콜백 도메인, 운영 배포

### 백엔드 확인 (2026-10-10 · PR #20)

- 기준: origin/main `f094e88`의 FC-021. `docs/DEPLOY.md`의 Apple 콜백을 `https://api.devkdk.com/api/v1/auth/apple/callback`으로 통일했다.
- 실제 외부 확인: `api.devkdk.com/actuator/health` HTTP 502, `api.jungsan.devkdk.com` DNS 이름 없음. 502만으로 OCI 내부 원인은 확정하지 않았다.
- 운영 접속: CTO가 지정한 OCI에 기존 키로 접속했다. 정산어택 배포 디렉터리는 없고, 8080은 기존 n8n Kafka 앱이 사용 중이다. 정산어택은 별도 포트가 필요하다.
- PR #18·#20·#21은 확인 시점 모두 병합 전이다. PR CI 성공은 OCI 배포가 아니다.
- 웹 원격 main의 `src/jeongsan/v3/api.ts`가 `/api/v1`을 붙이므로 Vercel 값은 `https://api.devkdk.com`이다. Production 재배포가 필요하다.
- 남은 작업: 운영 접근 복구·502 원인 확인, OCI 설정과 카카오/Apple 콘솔 등록, 사용자 병합 후 배포, Vercel 설정·재배포, TestFlight/웹 실제 로그인 시험. 외부 설정을 저장하거나 운영을 배포한 상태로 보고하지 않는다.
