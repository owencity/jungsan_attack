# 아키텍처 결정 기록 (ADR)

스택이나 설계를 다시 논쟁하기 전에 여기를 먼저 읽는다.
결정을 **뒤집는 것은 가능하지만, 근거를 모른 채 뒤집지는 않는다.**

| # | 결정 | 상태 |
|---|---|---|
| [001](001-rational-not-bigdecimal.md) | 원부담 누적에 `BigDecimal`을 쓰지 않고 유리수를 직접 구현한다 | 확정 |
| [002](002-outbox-not-rabbitmq.md) | 비동기 알림에 메시지 브로커 대신 아웃박스 테이블을 쓴다 | 확정 |
| [003](003-participant-stays-on-web.md) | 참여자에게 앱 설치를 요구하지 않는다 | 확정 |
| [004](004-two-step-confirm.md) | 확정을 미리보기·수락 2단계로 나누고 `inputHash`로 경합을 막는다 | 확정 |
| [005](005-no-stored-settlement.md) | 계산 결과를 저장하지 않되 `paidAmount`로 차액을 낸다 | ⚠️ 일부 대체 ([015](015-immutable-confirmed-transfer-snapshot.md)) |
| [006](006-single-vm-no-kubernetes.md) | 단일 VM에 docker compose로 배포한다. K8s·ArgoCD·Jenkins를 쓰지 않는다 | ✅ 유효 (MVP 배포) |
| [007](007-backend-serves-og.md) | 공유 링크를 백엔드 호스트에 두고 OG 태그가 든 HTML로 응답한다 | 확정 |
| [008](008-join-concurrency.md) | 참여 동시성을 `gathering` 행 잠금과 복합 유니크로 나눠 막는다 | 확정 |
| [009](009-group-persistent-membership.md) | `Group`(영구 모임)을 `Gathering` 위에 얹는다. 이름은 바꾸지 않는다 | 확정 |
| [010](010-realtime-chat-netty-mongo.md) | 실시간 채팅은 Netty+WebSocket+MongoDB, REST 서버와 별도 프로세스 | ⏸️ 부분 보류 ([014](014-monolith-first-feature-package.md)) |
| [011](011-mysql-over-postgresql.md) | 핵심 트랜잭션 저장소를 MySQL로 (PG 벤치마크 프로젝트와는 별개 판단) | 확정 |
| [012](012-offload-heavy-stateful-stores.md) | MongoDB·Elasticsearch는 관리형 무료 티어로 오프로드/유예한다 | ⚠️ Redis 용도만 대체 ([016](016-redis-scope.md)) |
| [013](013-msa-spring-cloud-k3s.md) | Spring Cloud + k3s 위에서 MSA로 전환한다 | ⏸️ 보류 — k3s 부분만 발동 검토 중 ([018](018-activate-k3s.md)) |
| [014](014-monolith-first-feature-package.md) | MVP는 모놀리스로 간다. 내부는 feature 패키지로 나눈다 | 확정 |
| [015](015-immutable-confirmed-transfer-snapshot.md) | 확정된 송금 명세를 불변 스냅샷으로 저장한다 | 확정 |
| [016](016-redis-scope.md) | Redis를 도입하되 용도를 시도 제한·쿼터·토큰 무효화·캐시로 한정한다 | 확정 |
| [017](017-kafka-behind-outbox.md) | 아웃박스를 유지한 채 그 뒤에 Kafka를 붙인다 | 확정 |
| [018](018-activate-k3s.md) | `013`의 k3s 부분을 발동한다 (MSA 전환은 아님) | 🕓 제안 — Kamal 비교·메모리 실측 후 확정 |

> **`006` → `013` → `014` 이력.** `013`이 한때 `006`을 대체했으나, 그 근거가
> "실제 필요"가 아니라 **"학습 목적"** 이었다. `014`가 이를 뒤집어 MVP를 모놀리스로
> 되돌리고 `006`을 되살렸다. `010`·`013`은 **폐기가 아니라 보류**다 — 설계가 그대로
> 살아 있어서, `014`의 재검토 조건이 오면 새로 고민할 것 없이 실행하면 된다.
>
> 이 저장소의 구조는 **실제 필요를 따라간다.** 학습 목적으로 구조를 올리지 않는다.
>
> **`016`~`018`은 그 원칙 위에서 나왔다.** Redis(`016`)와 Kafka(`017`)는 새로
> 발명한 결정이 아니라 `ADR-012`가 이미 자리를 잡아두고 `ADR-002`가 재검토
> 조건까지 적어둔 것을, **조건이 실제로 충족돼서** 실행하는 것이다(영수증
> OCR·AI가 MVP로 들어오며 소비자와 외부 과금 호출이 생겼다). 반면 `018`(k3s)은
> **아직 반론이 살아 있어 제안 상태다** — `ADR-006`이 지정한 Kamal과 비교
> 실측하기 전에는 확정하지 않는다.

## 형식

각 문서는 **맥락 → 결정 → 근거 → 대가 → 재검토 조건** 순으로 쓴다.
**재검토 조건**이 가장 중요하다. 그 조건이 오면 결정을 다시 본다.
