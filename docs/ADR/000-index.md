# 아키텍처 결정 기록 (ADR)

스택이나 설계를 다시 논쟁하기 전에 여기를 먼저 읽는다.
결정을 **뒤집는 것은 가능하지만, 근거를 모른 채 뒤집지는 않는다.**

| # | 결정 | 상태 |
|---|---|---|
| [001](001-rational-not-bigdecimal.md) | 원부담 누적에 `BigDecimal`을 쓰지 않고 유리수를 직접 구현한다 | 확정 |
| [002](002-outbox-not-rabbitmq.md) | 비동기 알림에 메시지 브로커 대신 아웃박스 테이블을 쓴다 | 확정 |
| [003](003-participant-stays-on-web.md) | 참여자에게 앱 설치를 요구하지 않는다 | 확정 |
| [004](004-two-step-confirm.md) | 확정을 미리보기·수락 2단계로 나누고 `inputHash`로 경합을 막는다 | 확정 |
| [005](005-no-stored-settlement.md) | 계산 결과를 저장하지 않되 `paidAmount`로 차액을 낸다 | 확정 |
| [006](006-single-vm-no-kubernetes.md) | 단일 VM에 docker compose로 배포한다. K8s·ArgoCD·Jenkins를 쓰지 않는다 | 확정 |
| [007](007-backend-serves-og.md) | 공유 링크를 백엔드 호스트에 두고 OG 태그가 든 HTML로 응답한다 | 확정 |
| [008](008-join-concurrency.md) | 참여 동시성을 `gathering` 행 잠금과 복합 유니크로 나눠 막는다 | 확정 |

## 형식

각 문서는 **맥락 → 결정 → 근거 → 대가 → 재검토 조건** 순으로 쓴다.
**재검토 조건**이 가장 중요하다. 그 조건이 오면 결정을 다시 본다.
