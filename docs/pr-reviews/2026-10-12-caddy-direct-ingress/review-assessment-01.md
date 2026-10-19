# PR #32 실제 AI 리뷰 대조

원문: [AI 댓글](https://github.com/owencity/jungsan_attack/pull/32#issuecomment-6111308220), `github-pr32-02.json`.
확인한 head는 `8a519a7`이며 업무/인프라 source는 고정한 `04a4750` 이후 동일하다.
AI의 질문·잠재 위험과 확인된 코드 결함을 구분한다. 아래는 Codex 자체 대조이며 사람 승인 아니다.

| 지적 | 코드·운영 검증과 판단 |
|---|---|
| MEDIUM · forwarded headers 전략 확인 | `ProviderGateway.kt:58`는 명시한 redirect 설정을 쓰고 `AuthController.kt:23,33,51,69`는 `app.cookie-secure`로 Secure를 정한다. 실제 운영의 두 로그인 시작에서 HTTPS 콜백과 Secure/HttpOnly 쿠키를 확인했다. 현재 기능은 request.isSecure/getScheme을 기반으로 생성하지 않는다. 미래 스킴 의존 기능이 생기면 proxy 신뢰 경계를 함께 설계한다. 지금 전역 헤더 신뢰를 임의 추가하지 않는다. |
| MEDIUM · DNS only의 rate-limit/L7 보호 부재 | 기존 Cloudflare 보호를 우회하는 직접 연결의 유효한 트레이드오프다. ADR-021과 PR 위험에 기록했다. 일반 요청의 Caddy/Spring rate-limit 정책을 이번 PR에서 새로 만들지 않았다. 새 플러그인·connlimit은 실제 요청/동시성 기준·오탐·운영 비용과 함께 별도 결정할 범위다. 이 지적을 해결한 것으로 표시하지 않는다. |
| LOW · access log 비활성화로 관찰 제한 | 현재 개인정보 보호를 위해 접근 로그를 끄고 error 로그의 URI/headers까지 제거한다. 앱의 내부 Micrometer/health는 유지하지만 Caddy의 차단 트래픽과 모든 요청 지연을 관찰하는 대신은 아니다. 최소 메타데이터 로그는 개인정보 기준과 함께 후속 설계할 수 있으며 자동 감시를 구축했다고 쓰지 않는다. |
| LOW · 실행 사용자/설정·인증서 권한 | 실제 `systemctl show caddy`: User/Group=caddy. 설정은 644 root:root이며 caddy 사용자의 읽기 시험 성공. `/var/lib/caddy`는 750 caddy:caddy이고 같은 사용자의 certificate store 쓰기 시험 성공. root 소유 설정은 의도한 관리 권한 분리다. 실제 발급·reload·공개 HTTPS 모두 성공했으므로 소유권 불일치 결함으로 확정하지 않는다. |

추가 테스트 제안 중 공개 경로 외 `/admin`·`/api`·metrics의 404와 upstream 미도달, 실제 upstream 중단 후
502·오류 로그의 URI/headers/DUMMY_CODE/STATE/TOKEN/COOKIE 제거는 이미 실제 Caddy 테스트와 CI에서 통과했다.
scheme 자체를 신뢰한다고 주장하지 않았으므로 그런 테스트가 통과했다고 기록하지 않는다.

진행 완료: Caddy 설치·DNS 직접 연결·공인 TLS·proxy 기능·CORS·로그 제거·방화벽 재실행 검증.
후속: 실제 사용자 웹/앱 최종 로그인, 최초 연결 지연의 추가 관찰, 별도 운영 보호/관찰 정책.
개인 스터디 상태는 미완료다. 외부 PR 승인·스토어 심사 완료와 구분한다.
