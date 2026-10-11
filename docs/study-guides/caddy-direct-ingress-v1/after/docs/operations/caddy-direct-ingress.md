# OCI Caddy 설치·직접 HTTPS 전환·복구

대상은 `jungsan-api.devkdk.com`과 OCI Ubuntu 24.04 ARM VM `140.245.109.32`뿐이다.
2026-10-12 CTO의 Caddy 설치 승인에 따른다. 다른 프로젝트의 웹훅·Kafka·노트북 API는 변경하지 않는다.

## 운영 적용 확인 (2026-10-12 01:15~01:18 KST)

- 공식 패키지 Caddy `v2.11.7`, 서비스 active·enabled. 기존 cloudflared active를 유지했다.
- CTO가 OCI 인바운드와 DNS 전환을 진행했고 외부 HTTP 308·직접 HTTPS 200으로 실제 도달을 확인했다.
  공용 DNS 1.1.1.1·8.8.8.8의 A는 OCI IP, 별도 AAAA는 없었다.
- 공인 Let's Encrypt 인증서의 도메인·신뢰 검증을 통과했다. 만료는 2027-01-09 15:12:33 UTC다.
  검증을 생략하는 옵션은 사용하지 않았다. Caddy가 자동 갱신하며 80·443과 DNS 직접 연결을 유지한다.
- health 200·UP, 미인증 사용자 조회 401, 웹 Origin과 credentials CORS·OPTIONS 200,
  카카오·Apple 시작 302, 공개 metrics/Prometheus 404를 확인했다. Apple scope는 없고 state·nonce는 유지한다.
- 앱 이미지 `3eb5254`와 컨테이너의 2026-10-10 시작 시각을 유지했다. `.env`는 600이며 값을 출력하지 않았다.
- 방화벽 계획 테스트 8건, 실제 Caddy 임시 포트의 경로·POST/401·CORS/쿠키·장애 로그 제거 검증,
  실제 설치 재실행 시 두 규칙의 중복 방지, `:server:compileKotlin` 18초 성공을 확인했다.
- 설치 백업: `/var/backups/jeongsan-caddy/20261011T160351Z-1531599`.
  재실행 백업: `/var/backups/jeongsan-caddy/20261011T161829Z-1565448`.

측정 원문은 [검증 JSON](2026-10-12-caddy-public-verification.json)에 보존했다. 운영 토큰·쿠키·
인가 code/state 값은 JSON에 넣지 않았다. 아래 모든 요청의 peer는 OCI IP이고 CF-Ray는 없었다.

| 같은 Windows 클라이언트의 요청 | 측정 |
|---|---|
| 직접 연결 `/actuator/health` · 새 연결 3회 | 0.65~0.84초 |
| 직접 연결 `/api/v1/auth/me` · 새 연결 3회 | 0.75~2.00초 |
| 직접 연결 `/api/v1/auth/me` · 같은 프로세스 첫 연결 | 0.68초 |
| 같은 연결을 재사용한 다음 4회 | 0.12~0.47초 |
| 이전 Tunnel `/api/v1/auth/me` · 10/11의 새 연결 3회 | 0.98~1.38초 |

전환은 성공했지만 새 연결의 지연이 항상 개선됐다는 결론은 아니다. 이전 측정과 시각이 다르고
신규 연결에는 DNS·TCP·TLS 비용과 네트워크 변동이 포함된다. 연결 재사용 결과만 골라 최초 접속
속도로 보고하지 않는다. 실제 웹·앱 계정 로그인·배포 심사 통과는 이 확인에 포함되지 않는다.

## 1. 무엇이 달라지는가

```text
기존: 사용자 → Cloudflare LAX → OCI Tunnel → 127.0.0.1:18080 → Spring
전환: 사용자 → OCI Caddy:443 → 127.0.0.1:18080 → Spring
```

`docker-compose.prod.yml`의 app·mysql, 앱의 `.env`, 도메인과 OAuth 콜백은 유지한다.
Caddy는 호스트 서비스다. 일반 컨테이너의 `127.0.0.1`은 호스트 Spring 포트가 아니므로 컨테이너에
동일 upstream을 그대로 넣지 않는다. Caddy 설치만으로 DNS의 Tunnel 경유가 없어지는 것은 아니다.

## 2. 서버 설치

1. SSH·sudo, 앱 health UP, 80·443을 쓰는 다른 서비스가 없는지 확인한다.
2. 검토한 이 PR의 `infra/caddy/`와 `scripts/ingress/`를 같은 루트 아래 준비한다.
   현재 서버 준비 루트는 `/home/ubuntu/jeongsan/ingress-candidate`다. 앱 배포 디렉터리의 파일을 덮지 않는다.
3. `sudo bash scripts/ingress/install-caddy.sh`를 준비 루트에서 실행한다.
   공식 서명 apt 저장소를 사용하고 Caddy 설정을 검증한다. 기존 앱 배포와 `.deploy.lock`을 공유한다.
4. 설치 스크립트는 `/etc/caddy/Caddyfile`을 반영하고 Caddy를 enable·start/reload한다.
   다른 이름의 Caddy 설정·Nginx·Apache가 있으면 자동 교체하지 않는다.
5. `scripts/ingress/allow-caddy-ports.py`는 INPUT 최종 REJECT 앞에 TCP 80·443 허용만 추가하고
   `/etc/iptables/rules.v4`에도 두 규칙만 저장한다. SSH·OCI InstanceServices·Docker·Tailscale 규칙은 유지한다.
   전체 live rules를 영구 저장하거나 `iptables-restore`로 재적용하지 않는다.

백업은 `/var/backups/jeongsan-caddy/<UTC시각>-<PID>/`다. Caddy 설정·saved rules·live rules 참고 자료를
보존한다. live rules 파일을 그대로 restore하면 Docker/Tailscale의 현재 상태와 어긋날 수 있다.
필요하면 `jeongsan-caddy` 표시가 붙은 두 규칙만 제거하고 saved rules에서 두 줄만 제거한다.
방화벽 변경 중 실패하면 이 실행이 추가한 허용 규칙만 제거한다. SSH 허용을 제거하지 않는다.

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
systemctl is-active caddy cloudflared
sudo ss -tlnp
sudo iptables -S INPUT
curl --fail http://127.0.0.1:18080/actuator/health
```

설정은 `/api/*`와 정확히 `/actuator/health`만 전달한다. `/actuator/prometheus`·`/actuator/metrics`·
기타 경로는 404이며 관리 API는 `127.0.0.1:2019`다. 접근 로그는 켜지 않으며 오류 로그의 URI·헤더도
제거한다. 로그에 OAuth code·state·토큰·쿠키를 보존하지 않는다. HTTPS는 HTTP/1.1·HTTP/2로 제공한다.

## 3. OCI 콘솔 인바운드

VM → Attached VNIC → 적용되는 Security List/NSG에 아래 stateful ingress를 추가한다.
기존 SSH·웹훅 규칙을 지우거나 수정하지 않는다. 기본이 Reject인 호스트 방화벽도 2절대로 함께 준비한다.

| 소스 | IP protocol | Destination port |
|---|---|---|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |

80은 자동 HTTPS 리다이렉트와 ACME HTTP-01 검증에 사용한다. UDP 443은 이번 설정에 필요 없다.
OCI 콘솔 권한은 현재 Codex에 없으므로 CTO가 추가한다. 외부 연결 시험으로 실제 적용을 확인한다.

## 4. DNS 전환 — 토큰 없이 화면에서 가능

Cloudflare → `devkdk.com` → DNS → Records에서 **`jungsan-api` 한 항목만** 수정한다.
먼저 기존 CNAME 대상 `<기존 OCI Tunnel UUID>.cfargotunnel.com`, TTL, proxy 상태를 메모한다.

| 항목 | 전환 값 |
|---|---|
| Type | `A` |
| Name | `jungsan-api` |
| IPv4 address | `140.245.109.32` |
| Proxy status | `DNS only` · 회색 구름 |
| TTL | Auto 또는 허용되는 짧은 TTL |

Tunnel을 가리키는 CNAME의 구름만 회색으로 바꾸는 것은 OCI 직접 연결이 아니다.
동일 호스트의 중복 A/AAAA가 있으면 자동 삭제하지 말고 원래 값과 영향을 확인한다.
`api.devkdk.com`·`webhook.devkdk.com`·웹 주소는 그대로 둔다. OCI Tunnel의 기존 정산 API 경로도
복구용으로 남겨 둔다. Zero Trust에서 Public Hostname을 삭제하면 DNS까지 지워질 수 있어 삭제하지 않는다.

**토큰은 API로 DNS를 자동 수정할 때만 필요하다.** 이 설치는 Caddy의 표준 HTTP-01/TLS-ALPN을 쓴다.
현재 저장소 `Cloudflare API DNS` workflow는 이전 Tunnel CNAME 전용이며 직접 연결 중 실행하지 않는다.
기존 스크립트는 A 레코드를 CNAME으로 덮어쓰지 않고 거절한다.

## 5. 인증서와 운영 검증

DNS가 아직 Tunnel을 가리키면 HTTP/TLS 검증 요청은 OCI Caddy에 도달하지 않아 발급이 실패한다.
DNS 전환 직후 인증서 발급이 끝나기 전 잠시 TLS 오류가 날 수 있다. DNS 전환 전에 공인 인증서가
발급됐다고 보고하지 않는다. DNS가 OCI를 가리키는 것을 확인한 뒤 최초 발급 재시도를 확인한다.
재시도 간격 때문에 지연되면 현재 인증서가 없음을 확인하고 Caddy만 재시작한다. 앱/DB/Tunnel은 재시작하지 않는다.

검증에서는 `curl --insecure`를 사용하지 않는다. `--resolve`는 DNS 캐시 영향을 분리할 뿐 인증서 검증을 유지한다.

```bash
curl --resolve jungsan-api.devkdk.com:443:140.245.109.32 https://jungsan-api.devkdk.com/actuator/health
curl -i https://jungsan-api.devkdk.com/api/v1/auth/me
curl -i https://jungsan-api.devkdk.com/actuator/prometheus
```

- 공인 인증서의 호스트 일치·인증 경로 검증, health 200·UP, 미인증 `/api/v1/auth/me` 401을 확인한다.
- 직접 연결 응답에 CF-Ray가 없고 원격 IP가 OCI인지 확인한다. HTTP → 같은 도메인 HTTPS 리다이렉트도 확인한다.
- 웹 Origin으로 CORS 응답, 카카오/Apple 인가 요청, 웹·앱의 실제 계정 로그인을 별도로 확인한다.
- 내부 관리·metrics 경로가 외부에 공개되지 않는지 확인한다.
- 같은 클라이언트·실패하지 않는 같은 경로로 여러 요청을 비교한다. DNS/TCP/TLS/TTFB/전체 시간을
  따로 기록하고 차가운 최초 연결과 재사용 연결을 구분한다. SSH TCP RTT만으로 HTTPS 성능을 확정하지 않는다.

`/api/v1/me`는 현재 핸들러가 없는 경로이며 500 오매핑 진단은 [PR #31](https://github.com/owencity/jungsan_attack/pull/31)에
보존했다. 이번 Caddy 설치는 앱의 예외 매핑을 고치지 않는다. 정상 사용자 조회는 `/api/v1/auth/me`다.
FC-021의 웹/App 실계정 시험을 이 인프라 준비만으로 완료 처리하지 않는다.

## 6. 복구

직접 HTTPS 검증에 실패하면 Cloudflare에서 메모한 기존 CNAME 대상·proxied=true·TTL을 복원하고,
기존 Tunnel을 통한 health·401을 다시 확인한다. 기존 Tunnel 경로를 남겼으므로 앱을 옮길 필요가 없다.
DNS 캐시 때문에 이전/새 경로가 잠시 섞일 수 있다. 도메인·`.env`·DB·계정 키는 변경하지 않는다.

근거: [Caddy 설치](https://caddyserver.com/docs/install),
[자동 HTTPS 조건](https://caddyserver.com/docs/automatic-https),
[Caddy 서비스](https://caddyserver.com/docs/running),
[Cloudflare 프록시 상태](https://developers.cloudflare.com/dns/proxy-status/).
