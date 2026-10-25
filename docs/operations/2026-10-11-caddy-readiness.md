# 2026-10-11 OCI 직접 HTTPS 연결 점검

## 결론

OCI에 Nginx·Caddy·Apache가 없고 TCP 80/443, UDP 443도 비어 있다. Caddy를 추가할 때 기존 웹서버와의 포트 충돌은 확인되지 않았다.
그러나 호스트 방화벽은 공인 경로의 TCP 80/443을 허용하지 않고, 외부 연결도 실패한다.
**Caddy 설치 + 호스트/OCI 인바운드 허용 + 해당 API 이름의 직접 DNS 전환 + 인증서/회귀 확인**이 함께 필요하다.
Caddy 설치만으로 현재 Cloudflare 경유가 사라지지는 않는다.

이 문서는 읽기 전용 점검 결과와 전환 제안이다. Caddy 설치, 방화벽·DNS·터널 수정, 앱 재배포는 실행하지 않았다.

## 확인한 상태

확인 시각: 2026-10-11 01:10~01:14 KST. 운영 이미지 `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`.
OCI 공인 주소는 기존 `OCI_HOST` 설정을 사용한다. 현재 터널로 가린 주소를 이 문서에 새로 공개하지 않는다.

| 항목 | 실제 확인 결과 |
|---|---|
| OCI 리전 | 인스턴스 메타데이터 `ap-singapore-1`, 싱가포르 |
| 인스턴스 | ARM `VM.Standard.A1.Flex` |
| TCP 80/443 리스너 | 없음 |
| UDP 443 리스너 | 없음 |
| Nginx/Caddy/Apache | systemd unit `not-found`, 실행 파일 없음 |
| Certbot | 실행 파일 없음 |
| cloudflared | active |
| 정산어택 app | `127.0.0.1:18080 → 8080`, 현재 운영 이미지 |
| MySQL | 공인 호스트 포트 없음 |
| 기존 웹훅 | 다른 app이 `127.0.0.1:8080` 사용, 유지 대상 |
| 호스트 방화벽 | INPUT은 기존 연결·loopback·SSH 등을 허용한 뒤 REJECT. 공인 TCP 80/443 허용 규칙 없음 |
| Tailscale 규칙 | tailscale0 및 해당 로컬/UDP 경로 허용. 공인 TCP 80/443을 열어주는 규칙 없음 |
| UFW/OCI CLI | 실행 파일 없음 |
| 현재 API DNS | Cloudflare anycast A/AAAA 주소로 응답 |
| 외부 80/443 접속 | 각 3회, 매번 약 3초 제한으로 timeout |

OCI Security List/NSG의 인바운드 규칙은 이번 SSH 점검만으로 확인하지 못했다.
외부 timeout이 호스트 방화벽과 OCI 규칙 중 어느 지점에서 발생하는지 이 결과만으로 구분하지 않는다.

## 성능 근거와 한계

00:34 KST 제어한 비로그인 요청 5회씩의 Micrometer 처리 평균:

| 요청 | 앱 처리 평균 | OCI loopback 왕복 |
|---|---|---|
| `/api/v1/auth/me` | 1.200ms | 1.877~3.543ms |
| `/actuator/health` | 0.988ms | 1.834~2.679ms |
| `/api/v1/me` | 3.740ms, 현재 없는 경로 처리 버그로 500 | 4.073~6.356ms |

외부 HTTP 9회에서 CF-Ray가 모두 `-LAX`였다. 첫 TLS 지연 요청을 제외한 전체 응답 시간은 0.854~1.394초.
첫 요청은 TLS 완료까지 누적 4.992초, 전체 5.975초였다.
01:13 KST의 동일 클라이언트→OCI 공인 IP TCP 22 직접 연결은 3회 107.921/116.380/150.413ms였다.

**관측된 앱 밖 지연을 줄이기 위해 직접 경로를 시험할 근거는 있다.**
다만 22번 포트의 TCP 연결 시간은 Caddy의 HTTPS 전체 응답 시간이 아니다.
LAX 선택 원인, 구간별 전송·대기 시간, 다른 사용자의 네트워크 경로는 확인하지 않았다.
Caddy 적용 전 특정 속도 개선 비율이나 100ms HTTP 응답을 보장하지 않는다.

## 목표 구성

```text
현재: 사용자 → Cloudflare LAX(관측) → OCI cloudflared → 127.0.0.1:18080 Spring
제안: 사용자 → OCI 싱가포르 Caddy:443              → 127.0.0.1:18080 Spring
```

같은 `https://jungsan-api.devkdk.com`을 사용한다면 앱 주소·카카오/Apple 콜백 도메인·웹 쿠키/CORS 계약을 바꿀 필요가 없다.
DNS 이름의 목적지만 바꾸며 인증 제공자의 등록 주소는 유지한다.

Cloudflare의 DNS 역할은 계속 사용하되, **정산 API 레코드만 A → OCI 공인 IPv4, DNS only**로 전환해야 한다.
터널 CNAME을 그대로 두고 프록시 표시만 끄는 것으로 직접 OCI 연결을 만드는 것은 아니다.
같은 이름의 충돌 레코드, 프록시가 남은 레코드, 사용하지 않는 AAAA도 함께 확인한다.
[Cloudflare 공식 문서](https://developers.cloudflare.com/dns/proxy-status/)는 DNS only일 때 원점 IP로 직접 연결되고, Proxied일 때 Cloudflare를 통과한다고 설명한다.

`api.devkdk.com`과 `webhook.devkdk.com`은 변경 대상이 아니다.
cloudflared 프로세스와 기존 웹훅 터널을 끄지 않는다. 정산 API의 기존 터널 경로는 되돌리기 위해 보존한다.
API의 직접 경로에는 Cloudflare WAF·요청 제한이 적용되지 않으므로, 기존 에지 규칙을 확인하고 필요한 보호를 별도로 보장한다.

## Caddy 설정 후보

현재 Spring은 호스트 loopback에 바인딩되어 있으므로 호스트의 systemd Caddy에서 연결하는 구성이 간단하다.
일반 Docker 브리지의 Caddy 컨테이너에서 `127.0.0.1:18080`은 그 컨테이너 자체를 가리키므로 이 설정을 그대로 복사하지 않는다.

```caddyfile
jungsan-api.devkdk.com {
    handle /actuator/health {
        reverse_proxy 127.0.0.1:18080
    }

    handle /api/* {
        reverse_proxy 127.0.0.1:18080
    }

    handle {
        respond "Not found" 404
    }
}
```

이는 미설치 상태에서 준비한 후보이며 `caddy validate`를 실행한 결과가 아니다.
외부에는 API와 health만 전달하고 상세 지표는 OCI 내부에서 읽는다.
TLS의 자동 발급·갱신과 HTTP→HTTPS 전환은 Caddy가 관리한다.
표준 HTTP/TLS 인증 방식에는 올바른 DNS와 외부 80/443 접근이 필요하다.
[Caddy HTTPS 준비 조건](https://caddyserver.com/docs/quick-starts/https),
[reverse_proxy 동작](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)을 기준으로 작성했다.

OAuth 콜백의 query/form, 메서드, 요청 본문, Host와 쿠키를 보존해야 한다.
인가 code, ticket, token, 쿠키, 계좌 내용을 원문 access log에 추가하지 않는다.

## 적용 순서와 검증

1. 현재 해당 DNS 레코드·터널 경로·호스트 방화벽 규칙을 복구 가능한 형태로 보존한다. 토큰과 비밀 값은 문서에 넣지 않는다.
2. OCI Security List/NSG와 호스트 방화벽에서 TCP 80/443을 허용한다. 기존 SSH·Tailscale·웹훅 규칙은 유지한다.
3. Caddy를 설치하고 후보 설정을 검증한다. 인증서 저장소의 지속성과 서비스 자동 시작·갱신을 확인한다.
4. 공개 DNS 전환 전 인증서 준비 방법을 정한다. DNS-01 사전 발급은 DNS 권한과 그 방식의 설정이 필요하다. 기본 HTTP/TLS 방식으로 전환 후 발급한다면 초기 발급·전파 구간의 실패 가능성을 명시한다.
5. 정확히 `jungsan-api.devkdk.com`만 OCI A/DNS only로 전환한다. 정상 인증서 검증과 TCP 443 직접 접근이 성공해야 한다.
6. health 200, 비로그인 `/api/v1/auth/me` 401, 웹 credentials CORS, 카카오/Apple 인가와 실제 콜백, 앱 티켓 교환을 확인한다.
7. 같은 클라이언트에서 새 연결/재사용 연결을 구분하여 최소 10회씩 측정한다. CF-Ray 유무, TLS 검증, 첫 응답/전체 시간과 OCI 앱 처리 지표를 비교한다.
8. 기존 웹훅·노트북 API가 유지되는지 확인한다. DNS/TLS/로그인 실패 시 보존한 해당 API 터널 DNS로 되돌리고 health·콜백을 확인한다.

HTTPS가 준비되기 전 DNS를 바꾸면 현재 동작하는 로그인도 끊길 수 있으므로 인증서 준비 단계를 생략하지 않는다.
직접 프록시 전환은 없는 API의 500→404 수정과 별개다. 현재 `GlobalExceptionHandler`의 오류 분류는 후속 수정 대상이다.

## 기록 범위

제어한 진단 결과는 로컬 작업 기록의 `oci-ingress-20261011.json`, `oci-public-ports-20261011.json`,
`me-incident-origin-20261011.jsonl`, `me-incident-public-20261011.json`에 남겼다.
원문에는 이번 점검용 요청만 있고 인증 키·쿠키·토큰은 출력하지 않았다.
공인 IP·내부 네트워크 등의 불필요한 운영 정보를 공개 저장소에 복사하지 않고 위 표로 필요한 결과를 요약했다.
