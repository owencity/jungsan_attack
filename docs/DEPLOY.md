# 배포 — OCI + GitHub Actions

`ADR-006`(단일 VM + docker compose)을 따른다. K8s·ArgoCD·Jenkins를 쓰지 않는다.

```
GitHub Actions (ARM 러너)                  OCI Ubuntu
  ┌────────────────────────┐   SSH/SCP    ┌──────────────────────────┐
  │ core 테스트 (46개)       │ ───────────▶ │ docker load               │
  │ bootJar 빌드            │              │ docker compose up -d      │
  │ 도커 이미지 빌드          │              │   ├ app   (127.0.0.1:8080)│
  │ 이미지 tar.gz 전송       │              │   └ mysql (포트 비공개)     │
  │ 헬스체크 확인            │ ◀─────────── │ /actuator/health          │
  └────────────────────────┘              └──────────────────────────┘
```

**레지스트리(GHCR)를 쓰지 않는다.** 이미지를 tar 로 말아 SCP 로 보낸다 —
서비스가 하나뿐이라 레지스트리 인증·권한 설정이 얻는 것보다 비용이 크다.
서비스가 늘면 그때 GHCR 로 옮긴다.

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

```bash
ssh ubuntu@<OCI_HOST>
mkdir -p ~/jeongsan && cd ~/jeongsan
cat > .env <<'EOF'
DB_PASSWORD=<강한 비밀번호>
KAKAO_CLIENT_ID=<REST API 키>
KAKAO_CLIENT_SECRET=<카카오 로그인 시크릿>
KAKAO_REDIRECT_URI=https://api.devkdk.com/api/v1/auth/kakao/callback
JWT_SECRET=<32자 이상 랜덤>
FRONTEND_ORIGIN=https://jungsan.devkdk.com
LOGIN_SUCCESS_URL=https://jungsan.devkdk.com/jungsan
EOF
chmod 600 .env
```

`JWT_SECRET` 생성:
```bash
openssl rand -base64 48
```

> **로컬 개발용 값을 그대로 쓰지 않는다.** `application.yml` 의 기본값
> (`local-only-dev-secret-...`)은 저장소에 공개돼 있어 그대로 쓰면 누구나 토큰을 위조한다.

### (d) 카카오 콘솔에 운영 Redirect URI 추가

REST API 키 수정 → 카카오 로그인 리다이렉트 URI 에 추가:
```
https://api.devkdk.com/api/v1/auth/kakao/callback
```
로컬용(`http://localhost:8080/...`)은 그대로 두고 **한 줄 더** 넣는다.

### (e) 쿠키 `secure` 켜기 ⚠️

`AuthController.kt` 의 `.secure(false)` 를 운영에서는 `true` 로 바꿔야 한다.
현재 TODO 로 남아 있다 — HTTPS 붙인 뒤 처리한다.

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
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
curl -s localhost:8080/actuator/health
```


## backend-v4 배포 전 추가 조건

- 운영 `.env`에 독립된 32바이트 AES 키의 Base64 값 `PAYOUT_ENCRYPTION_KEY`를 추가한다. 저장소의 공개 로컬 키를 사용하지 않는다.
- 키를 바꾸면 기존 계좌 암호문을 읽을 수 없으므로 백업·키 회전 절차 없이 교체하지 않는다. 원문/키는 로그·PR에 쓰지 않는다.
- v1 CONFIRMED 자료가 있으면 024가 기동을 중단한다. 운영 DB 현황과 별도 자료 처리 결정을 먼저 검토한다.
- MySQL fresh migration과 기존 OPEN 자료 업그레이드 검증은 구분한다. 새 코드가 배포 가능한지 체크리스트와 PR 검증 결과를 확인한다.
- 사용자 병합 전에는 운영 배포를 실행하지 않는다.
