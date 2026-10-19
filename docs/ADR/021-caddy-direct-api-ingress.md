# ADR-021 — 정산 API는 OCI Caddy로 직접 HTTPS 연결한다

- 상태: CTO가 설치 승인, DNS·공인 인증서·운영 성능 확인은 별도
- 결정일: 2026-10-12

## 문제

기존 구성에는 Nginx가 없으며 Cloudflare Tunnel이 HTTPS 진입점이었다. 2026-10-11 공개 요청의
CF-Ray는 LAX였고 `/api/v1/auth/me` 401 응답은 0.98~1.38초였다. 같은 시점 Spring의 신규 측정은
Micrometer 요청 5건 평균 약 1.20ms였다. 병목 대부분은 앱 바깥이지만 LAX가 유일한 원인이라고
단정하지 않으며, 개선 폭은 직접 HTTPS 전환 후 같은 환경에서 다시 측정한다.

## 고려한 대안

- Tunnel 유지: origin을 공개하지 않고 기존 인증서 관리를 유지하지만 현재 경유 경로를 직접 제어하기 어렵다.
- Nginx와 별도 인증서 갱신 도구: 가능하지만 현재 서버에 Nginx가 없고 두 설정을 운영해야 한다.
- Caddy 호스트 서비스: 같은 도메인의 자동 HTTPS와 loopback reverse proxy를 한 서비스에서 관리한다.

## 선택

CTO가 Caddy 설치를 선택했다. 공식 Ubuntu 패키지를 설치하고 `/etc/caddy/Caddyfile`에서
`jungsan-api.devkdk.com`의 `/api/*`와 `/actuator/health`만 `127.0.0.1:18080`으로 전달한다.
MySQL·앱 컨테이너·`.env`·OAuth 콜백 URL은 유지한다. Caddy의 관리 API는 loopback에만 둔다.
TCP 80·443만 열며 HTTP/3용 UDP는 열지 않는다. 접근 로그를 켜지 않고 오류 로그에서도 요청
URI·헤더를 제거해 OAuth code·state·쿠키가 새 진입점의 로그에 남지 않도록 한다.

기존 OCI 방화벽은 SSH·Tailscale·Docker를 보존하며 최종 INPUT 거절 앞에 두 허용 규칙만 추가한다.
`rules.v4`에도 같은 두 규칙만 저장하고 전체 live rules를 restore/save하지 않는다.
DNS는 해당 호스트만 A·OCI IP·DNS only로 전환한다. 기존 노트북 API와 웹훅 Tunnel은 수정하지 않는다.
이 작업은 Spring 업무 로직·Gradle 의존성을 바꾸지 않으며 Java/Kotlin 중복 구현 대상이 아니다.

## 아쉬운 점과 복구

origin IP가 공개되고 Cloudflare 프록시의 보호·캐시를 우회한다. 단일 VM 장애 경계는 그대로다.
DNS가 아직 Tunnel을 가리키면 표준 Caddy의 HTTP-01/TLS-ALPN 인증서 발급은 성공하지 않는다.
전환 직후 발급이 끝나기 전 일시적인 TLS 실패가 가능하므로 운영 연결 완료로 보고하지 않는다.
실패 시 보관한 원래 CNAME·Tunnel UUID·proxied=true를 복원한다. 기존 Tunnel 경로는 남겨 둔다.
직접 연결 중 기존 Cloudflare API DNS workflow를 실행하지 않는다.

공식 근거: [설치](https://caddyserver.com/docs/install),
[자동 HTTPS의 DNS·포트 조건](https://caddyserver.com/docs/automatic-https),
[reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy),
[Cloudflare DNS only](https://developers.cloudflare.com/dns/proxy-status/).
