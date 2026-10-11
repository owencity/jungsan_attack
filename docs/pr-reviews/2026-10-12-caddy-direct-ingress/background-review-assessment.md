# 기존 진단 PR #31 AI 리뷰를 이번 설치에 대조

원문은 `github-pr31-background.json`의 실제 issue comment 2개다. 아래는 Codex의 대조·자체 판단이며 CTO/사람 승인이나 PR #32의 AI 리뷰가 아니다.

| 지적 | 실제 근거·이번 처리 |
|---|---|
| DNS 전환과 ACME 발급 사이 TLS 실패 가능성 | 타당하다. DNS 토큰 없는 표준 ACME 전환의 초기 실패 가능성을 사전에 기록했다. 외부 80 도달을 먼저 확인하고 CTO의 DNS 변경 후 Caddy만 최초 발급을 재시도했다. 최종 공인 인증서 검증·health 200/401은 실제 성공이다. 사전 발급·무중단이라고 쓰지 않는다. |
| health 후행 슬래시·health* 매칭 제안 | 정확한 `/actuator/health`가 현재 배포·외부 검증 경로다. 불필요하게 범위를 넓히지 않았고 `/actuator/health/extra` 등은 404·upstream 미도달 테스트로 검증했다. 모니터링 도구도 정확한 경로를 쓴다. |
| 원본 IP 노출·Cloudflare 보호 우회 | 직접 DNS 연결의 실제 트레이드오프다. ADR/PR에 기록했다. 이 지적만으로 새 rate-limit 플러그인·기능을 임의 도입하지 않는다. 새 운영 보호 정책·서비스 분리는 후속 결정이다. |
| 미등록 `/api/v1/me` 500은 문서만으로 수정되지 않음 | 맞다. PR #31 설명 노트와 별도 앱 오류 수정 범위를 구분했다. 이번 정상 사용자 조회 검증은 실제 `/api/v1/auth/me` 401이다. Caddy가 없는 핸들러를 만들어 준다고 보고하지 않았다. |
| Forwarded 헤더를 인식하지 않으면 OAuth·Secure 쿠키가 깨질 수 있음 | 조건부 우려다. 현재 `ProviderGateway.kt:58`의 redirect_uri는 명시한 운영 설정이고 `AuthController.kt:22,31,48,67`의 secure는 `app.cookie-secure`다. 실제 두 제공자의 시작 302에서 HTTPS 콜백·Secure/HttpOnly 바인딩 쿠키를 확인했다. 요청 스킴으로 URL/secure를 자동 추론하지 않으므로 현재 코드에 프록시 신뢰 설정을 추가할 근거로 삼지 않았다. 최종 실계정 콜백은 후속이다. |
| 미래 WebSocket 경로가 /api 밖이면 차단 | 미구현 경로에 대한 후속 설계 조건이다. 현재 계약 밖 `/ws/*`를 미리 공개하지 않는다. 실제 도입 PR에서 경로·권한과 Caddy proxy를 함께 검증한다. |
| 미래 404 예외 핸들러 추가 시 기존 핸들러 유지 | 별도 앱 수정의 회귀 조건으로 유효하다. 이번 PR에서 GlobalExceptionHandler를 수정하거나 기존 핸들러를 삭제하지 않았다. |

검증 코드의 실제 증거는 `scripts/verify-caddy-ingress.py`와 `scripts/verify-caddy-firewall.py`, 운영 원문은 `docs/operations/2026-10-12-caddy-public-verification.json`이다. 대안·설계 질문과 확인된 결함을 구분한다.
