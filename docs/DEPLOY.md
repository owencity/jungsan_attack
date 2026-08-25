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

- `main` 에 push → 자동 배포 (문서·design 만 바뀌면 건너뜀)
- 수동: Actions → **Deploy to OCI** → Run workflow

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
