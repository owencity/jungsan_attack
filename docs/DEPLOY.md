# 배포 — OCI + GitHub Actions

`ADR-006`(단일 VM + docker compose)을 따른다. K8s·ArgoCD·Jenkins를 쓰지 않는다.

**운영 DB 최초 인증 (2026-10-10):** MySQL 8.4의 인증 캐시가 비어 있을 때도 연결되도록
`application-prod.yml`의 JDBC URL에 `allowPublicKeyRetrieval=true`를 명시한다. 현재 MySQL은
외부 포트를 열지 않는 단일 VM compose 내부 서비스다. 이 설정은 서버 신원 검증을 대신하지 않으므로
DB를 외부·공유 네트워크로 옮기기 전 TLS와 인증서 검증을 갖춘다.
[Connector/J 보안 설정](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html)과
[MySQL 8.4 인증 규칙](https://dev.mysql.com/doc/refman/8.4/en/caching-sha2-pluggable-authentication.html)을 참고했다.
CI는 `FLUSH PRIVILEGES` 뒤 실제 prod 프로필을 띄워 처음부터 검증한다. 보존하는 prod 로그는 CI의
가짜 설정으로 기동한 서버 로그이며 OCI 운영 로그·실제 키는 업로드하지 않는다.

```
GitHub Actions (ARM 러너)                  OCI Ubuntu
  ┌────────────────────────┐   SSH/SCP    ┌──────────────────────────┐
  │ core·server 테스트      │ ───────────▶ │ docker load               │
  │ bootJar 빌드            │              │ docker compose up -d      │
  │ 도커 이미지 빌드          │              │   ├ app   (127.0.0.1:18080)│
  │ 이미지 tar.gz 전송       │              │   └ mysql (포트 비공개)     │
  │ 헬스체크 확인            │ ◀─────────── │ /actuator/health          │
  └────────────────────────┘              └──────────────────────────┘
```

**레지스트리(GHCR)를 쓰지 않는다.** 이미지를 tar 로 말아 SCP 로 보낸다 —
서비스가 하나뿐이라 레지스트리 인증·권한 설정이 얻는 것보다 비용이 크다.
서비스가 늘면 그때 GHCR 로 옮긴다.

> **운영 주소 확정 (2026-10-10):** `https://jungsan-api.devkdk.com`.
> `api.devkdk.com`은 노트북 터널의 다른 서비스이므로 변경하지 않는다.
> OCI의 기존 `webhook.devkdk.com` → 8080 규칙도 유지한다. 아래 새 호스트·콜백만 추가한다.

## 1회성 준비

### (a) GitHub Secrets

저장소 → Settings → Secrets and variables → Actions → Repository secrets

| Secret | 값 |
|---|---|
| `OCI_HOST` | OCI 공인 IP |
| `OCI_SSH_KEY` | 개인키 **전체 내용** (`-----BEGIN`~`-----END` 포함) |

`OCI_USER`(`ubuntu`)는 숨길 값이 아니라 워크플로에 직접 적었다.

> `OCI_HOST` 를 secret 에 두는 이유 — 저장소가 public 이고 Cloudflare 프록시로
> origin IP 를 가리고 있다. 워크플로에 IP 를 적으면 그 보호가 무의미해진다.

### (b) 러너 아키텍처 확인 ⚠️

```bash
ssh ubuntu@<OCI_HOST> uname -m
```

| 출력 | `deploy.yml` 의 `runs-on` |
|---|---|
| `aarch64` | `ubuntu-24.04-arm` (기본값) |
| `x86_64` | `ubuntu-latest` 로 **변경 필요** |

**틀리면 컨테이너가 `exec format error` 로 안 뜬다.** 이미지 아키텍처는
빌드한 러너를 따라가기 때문이다.

### (c) 서버에 `.env` 만들기

저장소에 두지 않는다(카카오 시크릿·DB 비밀번호). OCI 에서 직접 만든다.
2026-10-10 추가 결정: Codex가 `~/jeongsan/.env`를 생성하고 카카오 기존 설정과 새 내부 키를 채운다.
CTO가 Sign in with Apple 로그인용 Key ID·개인키를 서버에 직접 입력한다. 키 값은 저장소·채팅·로그에 남기지 않는다.

| 필요한 키 | 용도·입력 기준 |
|---|---|
| `DB_PASSWORD` | 정산어택 전용 MySQL 비밀번호 |
| `JWT_SECRET` | 새로 생성한 32자 이상 JWT 서명 비밀 |
| `PAYOUT_ENCRYPTION_KEY` | 계좌용 독립 32바이트 키의 Base64 |
| `AUTH_ENCRYPTION_KEY` | Apple 해제 작업용 독립 32바이트 키의 Base64 |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` | 카카오 운영 REST API 키·로그인 시크릿 |
| `KAKAO_REDIRECT_URI` | `https://jungsan-api.devkdk.com/api/v1/auth/kakao/callback` |
| `APPLE_CLIENT_ID`, `APPLE_TEAM_ID`, `APPLE_KEY_ID` | 웹 Services ID·Developer Team ID·Sign in with Apple Key ID |
| `APPLE_PRIVATE_KEY_BASE64` | 해당 Apple p8의 PEM 헤더/푸터를 제외한 Base64 본문 |
| `APPLE_REDIRECT_URI` | `https://jungsan-api.devkdk.com/api/v1/auth/apple/callback` |
| `FRONTEND_ORIGIN` | `https://jungsan.devkdk.com` |
| `LOGIN_SUCCESS_URL` | `https://jungsan.devkdk.com/jungsan` |

`APP_IMAGE`는 배포 스크립트가 해당 main SHA로 지정하므로 CTO가 `.env`에 넣지 않는다.
`DB_HOST/PORT/NAME/USER`와 `SPRING_PROFILES_ACTIVE`는 운영 compose에서 지정한다.
Cloudflare·OCI SSH 키는 아래 GitHub 설정이며 앱 `.env`에 넣지 않는다.

```bash
ssh ubuntu@<OCI_HOST>
mkdir -p ~/jeongsan && cd ~/jeongsan
cat > .env <<'EOF'
DB_PASSWORD=<강한 비밀번호>
KAKAO_CLIENT_ID=<REST API 키>
KAKAO_CLIENT_SECRET=<카카오 로그인 시크릿>
KAKAO_REDIRECT_URI=https://jungsan-api.devkdk.com/api/v1/auth/kakao/callback
JWT_SECRET=<32자 이상 랜덤>
PAYOUT_ENCRYPTION_KEY=<계좌용 독립 32바이트 키의 Base64>
AUTH_ENCRYPTION_KEY=<외부 인증 자격용 독립 32바이트 키의 Base64>
# Apple 로그인 설정 발급 전에는 아래 값을 비워 둔다
APPLE_CLIENT_ID=
APPLE_TEAM_ID=
APPLE_KEY_ID=
APPLE_PRIVATE_KEY_BASE64=
APPLE_REDIRECT_URI=https://jungsan-api.devkdk.com/api/v1/auth/apple/callback
FRONTEND_ORIGIN=https://jungsan.devkdk.com
LOGIN_SUCCESS_URL=https://jungsan.devkdk.com/jungsan
EOF
chmod 600 .env
```

Apple 웹 Services ID의 등록 도메인·Return URL과 위 callback을 맞춘다. 앱도 시스템 인증 브라우저에서 같은 Services ID 흐름을 사용한다.
`APPLE_PRIVATE_KEY_BASE64`는 PEM 헤더·푸터를 제외한 Base64 본문이다. 원본 p8은 저장소에 넣지 않는다.
Apple 키 발급 전에는 `APPLE_CLIENT_ID/TEAM_ID/KEY_ID/PRIVATE_KEY_BASE64` 값을 빈칸으로 둘 수 있다.
키 이름은 `.env`에 모두 선언하고 자리표시 문자열 대신 `APPLE_KEY_ID=`처럼 둔다.
이때 서버·카카오는 기동하며 Apple 요청은 503 `AUTH_PROVIDER_UNAVAILABLE`이다.
Apple 설정을 모두 채웠는데 개인키가 잘못되면 기동이 실패한다. 나머지 필수 운영 값에는 이 예외를 적용하지 않는다.
App Store Connect/TestFlight 배포용 p8은 로그인용이 아니므로 서버 설정에 넣지 않는다.
설정을 완성한 뒤 재배포해야 새 컨테이너가 Apple 값을 읽는다.
AUTH_ENCRYPTION_KEY는 JWT/PAYOUT 키와 별개이며 연결 해제 대기 작업이 있는 동안 변경하면 기존 토큰을 읽을 수 없다.
실제 Apple 로그인→앱 티켓 교환→탈퇴→Apple 연결 해제까지 확인한 뒤 앱 심사 자료에 결과를 기록한다.

`JWT_SECRET` 생성:
```bash
openssl rand -base64 48
```

> **로컬 개발용 값을 그대로 쓰지 않는다.** `application.yml` 의 기본값
> (`local-only-dev-secret-...`)은 저장소에 공개돼 있어 그대로 쓰면 누구나 토큰을 위조한다.

### (d) 카카오 콘솔에 운영 Redirect URI 추가

REST API 키 수정 → 카카오 로그인 리다이렉트 URI 에 추가:
```
https://jungsan-api.devkdk.com/api/v1/auth/kakao/callback
```
로컬용(`http://localhost:8080/...`)은 그대로 두고 **한 줄 더** 넣는다.

### (e) Apple Services ID의 웹 인증 설정

Certificates, Identifiers & Profiles → Identifiers → 운영 `APPLE_CLIENT_ID`와 같은 Services ID →
Sign in with Apple → Configure에서 기존 primary App ID와 연결된 웹 인증 설정을 확인한다.

| 항목 | 운영 값 |
|---|---|
| Domains and Subdomains | `jungsan-api.devkdk.com` |
| Return URLs | `https://jungsan-api.devkdk.com/api/v1/auth/apple/callback` |

Done → Continue → Save까지 저장한다. 등록 값과 OCI `.env`의 `APPLE_REDIRECT_URI`는 정확히 같아야 한다.
`api.jungsan.devkdk.com`은 현재 DNS가 없어 운영 콜백으로 사용하지 않는다.
문서 변경만으로 개발자 콘솔이나 OCI 설정이 바뀌지는 않는다.
[Apple 설정 절차](https://developer.apple.com/help/account/capabilities/configure-sign-in-with-apple-for-the-web)를 기준으로 확인한다.

### (f) 운영 쿠키·CORS 확인

`application-prod.yml`의 `app.cookie-secure: true`를 `AuthController`가 사용한다.
웹 JWT 쿠키는 `HttpOnly; Secure; SameSite=Lax`이고, Apple의 POST 콜백용 브라우저 바인딩 쿠키는
운영에서 `SameSite=None; Secure`다. 컨트롤러 소스의 값을 배포 때 직접 바꿀 필요가 없다.
`FRONTEND_ORIGIN`은 실제 웹 주소 `https://jungsan.devkdk.com`과 맞춘다.

### (g) Vercel 웹 API 연결

웹 `profile` 프로젝트 → Settings → Environment Variables에 아래 값을 Production 범위로 저장한다.

```text
VITE_JEONGSAN_API_BASE_URL=https://jungsan-api.devkdk.com
```

웹 `src/jeongsan/v3/api.ts`는 이 값에 `/api/v1/...`을 직접 붙인다. 값에는 `/api/v1`이나 끝의 `/`를 넣지 않는다.
환경변수 변경 뒤 Production을 다시 빌드·배포해야 브라우저의 번들에 반영된다.
[Vercel 환경변수 문서](https://vercel.com/docs/environment-variables)의 적용 범위·새 배포 규칙을 따른다.
백엔드의 `FRONTEND_ORIGIN`은 API 주소가 아니라 이 웹의 운영 origin이어야 한다.
API 복구·쿠키/CORS 확인 뒤 웹을 서버 모드로 전환한다.

## FC-021 운영 연결 확인 순서

1. OCI에 접속해 컨테이너 상태·로컬 `/actuator/health`·기동 실패 원인을 확인한다.
   외부 502만으로 앱 중단, 터널/프록시 오류, DB·환경변수 오류 중 하나를 확정하지 않는다.
2. 운영 DB 백업과 적용 changeSet 현황, v1 CONFIRMED 자료 유무, 필수 환경변수·키 준비를 확인한다.
   아래 backend-v4 배포 조건을 함께 확인한다. 값과 키를 로그·PR에 출력하지 않는다.
3. 위 카카오 Redirect URI, Apple Return URL, OCI 두 redirect 환경변수를 모두 같은 운영 호스트로 맞춘다.
4. 최신 CI 전체 통과·리뷰/스터디 보존·미결 CTO 결정 없음 조건으로 승인된 PR을 병합한 뒤 해당 main 커밋의 **Deploy to OCI**를 확인한다.
   `feat/auth-release`의 Backend CI 성공만으로는 운영 서버에 배포되지 않는다.
   병합 전 브랜치의 직접 운영 배포는 현재 절차에 포함되지 않는다.
5. 외부 `https://jungsan-api.devkdk.com/actuator/health`에서 200·UP을 확인하고,
   로그인 없이 `GET /api/v1/auth/me`가 401을 반환하는지 확인한다.
6. 웹 환경변수를 반영해 재배포하고 웹 카카오 로그인·로그아웃·탈퇴와 TestFlight 앱의
   카카오/Apple 로그인 → 앱 복귀 → 티켓 교환 → 인증 API까지 실제로 시험한다.

FC-021은 문서 수정, 콘솔 저장, 운영 배포, 웹 재배포, 실제 로그인 시험을 각각 기록하고
모두 확인한 뒤 닫는다. DNS 없는 Apple 콜백 주소를 문서에서 바꾼 것만으로 반영 완료 처리하지 않는다.

### Cloudflare DNS 실행과 Tunnel 연결

운영 API 도메인은 **`jungsan-api.devkdk.com` 하나**다. Apple·카카오 콜백에도 이 주소를 사용한다.
CTO는 GitHub Repository Secret `CLOUDFLARE_API_TOKEN`에 **devkdk.com DNS 편집 전용** 토큰을 넣는다.
GitHub Repository Variable `CLOUDFLARE_TUNNEL_ID`는 확인한 OCI 기존 Tunnel UUID다.
토큰에 Zone 조회 권한이 없으면 같은 화면의 Variable `CLOUDFLARE_ZONE_ID`에 devkdk.com Zone ID를 넣는다.
Zone ID를 지정한 경로는 Zone 조회 권한을 요구하지 않는다. API 토큰 값은 저장소나 출력에 기록하지 않는다.

CTO의 대시보드 추가가 DNS 레코드도 만들었다면 별도 DNS 작업은 필요 없다.
DNS 확인·보정이 필요할 때만 **새 호스트 수정 PR이 main에 병합된 뒤** **Cloudflare API DNS** workflow를 명시적으로 실행한다. 이 작업은 `jungsan-api.devkdk.com`의
CNAME을 `<기존 Tunnel UUID>.cfargotunnel.com`으로 맞추고 proxied=true를 확인한다. 같은 값이면 변경하지 않는다.
중복 레코드·다른 이름·A/AAAA 레코드는 임의 삭제/교체하지 않으며, 쓰기 뒤 레코드를 다시 조회한다.
PR에서는 이 동작을 가짜 API로만 시험한다. 다른 도메인·n8n·웹 DNS는 수정하지 않는다.

**DNS 편집 권한은 Tunnel 연결 포트를 편집하는 권한이 아니다.** 현재 OCI의 cloudflared는 원격 관리 방식이다.
CTO가 Cloudflare Zero Trust → Networks → Tunnels에서 OCI 커넥터의 Public Hostname에 **새 항목을 추가**한다.
Subdomain `jungsan-api`, Domain `devkdk.com`, Service `HTTP`, URL `localhost:18080`이다.
기존 `webhook.devkdk.com`의 `/webhooks/github` → `localhost:8080` 규칙과
노트북 터널의 `api.devkdk.com`은 그대로 둔다. 기존 항목을 편집하거나 교체하지 않는다.
DNS 성공·OCI 앱 health·외부 API health·실제 계정 로그인은 각각 따로 기록한다.
[Cloudflare DNS API](https://developers.cloudflare.com/api/resources/dns/subresources/records/methods/edit/)와
[Tunnel 관리 방식](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/)을 참고한다.

## 배포

- PR 생성·업데이트 → **Backend CI**에서 Core 테스트, Server 테스트, bootJar 빌드를 실행한다.
  대상 브랜치를 제한하지 않아 기능 브랜치 위에 쌓은 PR도 검증한다. 문서 전용 PR도 동일한 검사를 받아
  필수 검사 설정 시 경로 필터 때문에 대기 상태에 머무르지 않는다. 이 작업에는 운영 SSH 키가 필요 없다.
- CI의 테스트 XML·HTML은 실행 화면의 `backend-test-results`에서 14일 동안 받을 수 있다.
- `main` 에 push → 자동 배포 (문서·design 만 바뀌면 건너뜀)
- 배포 작업도 Core·Server 테스트를 다시 통과한 뒤 ARM 이미지 빌드·전송·기동·헬스체크를 진행한다.
- 수동: Actions → **Deploy to OCI** → Run workflow. `main`만 운영 배포하며 다른 브랜치는 건너뛴다.
- `gradlew`의 Git 실행 권한은 `100755`로 보존한다. Windows에서 파일 내용을 고쳐도 이 권한이
  유지되는지 확인한다. CI는 `test -x ./gradlew`로 확인하며 권한이 없으면 검사에 실패한다.

CI 성공은 운영 배포 성공과 다르다. **Deploy to OCI**의 헬스체크 성공까지 확인해야 배포 완료다.
실제 MySQL 경합·제공자 로그인·앱 연결 시험은 위 단위 테스트에 포함되지 않는다.

## 롤백

이미지에 태그를 안 붙여서 `docker` 만으로는 되돌릴 수 없다.
**이전 커밋으로 되돌린 뒤 다시 배포**한다.

```bash
git revert <문제 커밋> && git push
```

> `ADR-006` 이 인정한 대가다 — *"원클릭 롤백이 없다"*.
> 필요해지면 이미지에 커밋 SHA 태그를 붙이는 것부터 검토한다.

## 확인

```bash
ssh ubuntu@<OCI_HOST>
cd ~/jeongsan
APP_IMAGE=$(sed -n 's/^APP_IMAGE=//p' .release.env) docker compose -f docker-compose.prod.yml ps
APP_IMAGE=$(sed -n 's/^APP_IMAGE=//p' .release.env) docker compose -f docker-compose.prod.yml logs -f app
bash scripts/deploy/health.sh
```


## backend-v4 배포 전 추가 조건

- 운영 `.env`에 독립된 32바이트 AES 키의 Base64 값 `PAYOUT_ENCRYPTION_KEY`를 추가한다. 저장소의 공개 로컬 키를 사용하지 않는다.
- 키를 바꾸면 기존 계좌 암호문을 읽을 수 없으므로 백업·키 회전 절차 없이 교체하지 않는다. 원문/키는 로그·PR에 쓰지 않는다.
- v1 CONFIRMED 자료가 있으면 024가 기동을 중단한다. 운영 DB 현황과 별도 자료 처리 결정을 먼저 검토한다.
- MySQL fresh migration과 기존 OPEN 자료 업그레이드 검증은 구분한다. 새 코드가 배포 가능한지 체크리스트와 PR 검증 결과를 확인한다.
- 사용자 병합 전에는 운영 배포를 실행하지 않는다.
# 운영 준비 갱신 (2026-10-10)

실제 OCI의 8080은 기존 n8n Kafka 앱이 사용한다. 정산어택 compose project는 `jeongsan`,
앱은 `127.0.0.1:18080`에만 바인딩한다. OCI의 원격 관리 Tunnel에
`jungsan-api.devkdk.com` → `http://localhost:18080`을 새 항목으로 추가한다.
기존 n8n/Kafka 포트·컨테이너·이미지·볼륨은 변경하지 않는다.

이미지 태그는 `jeongsan-server:<main 40자리 SHA>`다. 성공한 태그만 서버 `.release.env`에 남기며,
헬스체크는 해당 compose 앱의 이미지·컨테이너 healthy·18080 바인딩·HTTP UP을 모두 검사한다.
첫 디렉터리는 파이프라인이 만들지만 **필수 운영 .env는 미리 준비해야 한다**. 키·설정이 없으면 출시가 끝난 것이 아니다.
실행 중인 배포는 취소하지 않으며 서버 잠금도 사용한다. 실패하면 이전 앱 이미지로 복원하고 DB는 유지한다.
새 migration은 이전 앱이 읽을 수 있는 추가 컬럼 방식으로 작성한다. 제거·타입 변경 migration은 별도 복구 계획이 필요하다.
이전 성공 이미지가 없는 최초 배포의 실패는 자동 복원이 불가능하다. 실패 원인을 고친 뒤 같은 SHA로 다시 배포한다.
수동 실행도 `bash scripts/deploy/deploy.sh <배포 디렉터리의 tar> jeongsan-server:<SHA>`를 쓴다.
운영 로그는 CI에 자동으로 덤프하지 않으며 OCI에서 필요한 부분만 확인한다.

## 실제 배포 확인 (2026-10-10 · PR #27)

[main 배포 실행](https://github.com/owencity/jungsan_attack/actions/runs/38030304729)은
테스트·실제 MySQL/HTTP 검증·이미지 빌드·SSH 동기화·전송까지 성공했다.
배포 단계는 OCI `~/jeongsan/.env` 부재로 중단됐고 정산어택 컨테이너·18080은 아직 없다.
SSH 인증 거절은 이 실행의 원인이 아니다. CTO가 위 필수 키와 **새 콜백 주소**를 넣고
`chmod 600 ~/jeongsan/.env`로 보호한 뒤 최신 main의 배포를 재실행한다.
Tunnel 추가·개발자 콘솔 등록·실제 로그인은 이 빌드 결과와 별도로 확인한다.

## 운영 설정 생성과 Tunnel 추가 (2026-10-10 · 추가 확인)

CTO가 OCI Tunnel에 `jungsan-api.devkdk.com` → `http://localhost:18080`을 추가했다고 확인했다.
Codex가 OCI `~/jeongsan/.env`를 권한 600으로 생성했다. 기존 DB·앱이 없는 것을 확인한 뒤
DB 비밀번호·JWT 서명·서로 독립인 계좌/인증 암호화 키를 새로 생성했고 기존 카카오 설정을 채웠다.
Apple 로그인용 설정은 발급 전 빈 값이다. 위 기존 배포 실패 기록은 파일 생성 전 시점이다.
Apple 미설정 기동 변경은 PR 검증·병합 뒤 배포하며, 실제 운영 health·카카오 로그인 성공은 따로 확인한다.
