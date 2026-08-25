# ADR-013 · Spring Cloud + k3s 위에서 MSA로 전환한다

**상태** ⏸️ **보류 — `ADR-014`의 조건 충족 시 발동** · **관련** `ADR-014`, `ADR-006`, `ADR-010`, `ADR-012`

> **이력:** 처음에는 "확정"이었고 `ADR-006`을 대체했다. 그런데 아래 맥락에 적힌
> 전환 근거가 *"학습 목적"* 이었다 — 실제로 아픈 곳이 생겨서가 아니었다.
> 2026-08-23 `ADR-014`가 이를 뒤집고 **MVP는 모놀리스 + `ADR-006` 배포**로 되돌렸다.
>
> **이 문서는 폐기가 아니다.** MSA로 갈 이유·서비스 경계·메모리 예산 계산이 여기
> 그대로 살아 있어서, `ADR-014`의 재검토 조건(프로세스가 2개 이상이 되어 배포·감시
> 대상이 늘어날 때)이 오면 **새로 고민할 것 없이 이 문서를 실행하면 된다.**

## 맥락

`ADR-006`은 *"비동기 처리는 FCM 하나뿐이고 인스턴스 한 대, 모놀리스라 K8s가 필요
없다"*를 전제로 단일 VM + docker compose를 결정했다.

이제 그 전제가 깨졌다. `ADR-010`이 채팅을 REST API 서버와 별도 프로세스(Netty
게이트웨이)로 이미 분리했고, 학습 목적으로 Spring Cloud + MSA + k3s 위에서
운영하기로 했다(사이드 프로젝트의 명시적 학습 목표).

**풀 스케일 MSA(서비스 5개 이상 + Eureka + Config Server)는 예산이 안 맞는다.**

```
k3s 컨트롤플레인                                    ~0.3~0.5GB
Config Server + Eureka(서비스 디스커버리)             ~0.6~1GB
API Gateway                                        ~0.3~0.5GB
서비스 5개 이상 (Group·Meeting·Chat·Notification·...)  ~1.5~2.5GB
MySQL · Redis · Kafka                              ~2.3~3.8GB
Prometheus + Loki + Grafana                        ~2~3GB
                                                    ─────────
                                                    ~7~11.3GB   ← 12GB 안에서 안정 여유가 없다
```

## 결정

**k3s(단일 노드)를 오케스트레이션으로 쓰되, 서비스는 `ADR-010`이 이미 나눈 경계
그대로 쓴다. 그 이상 쪼개지 않는다.**

```
① REST API 서비스        Group · Gathering · 정산 도메인 (Spring Boot MVC)
② 채팅 게이트웨이          Netty + WebSocket (ADR-010)
③ 알림 워커 (선택)         Outbox → Kafka 컨슈머 → FCM/채팅 브로드캐스트
```

```
Cloudflare → k3s (Spring Cloud Gateway) → ① · ② · ③
```

### Eureka·Config Server는 쓰지 않는다

**k3s 위에서는 그 역할이 이미 있다.**

```
서비스 디스커버리   Eureka 대신 → k3s Service (DNS 기반, "meeting-api.default.svc")
설정 관리          Config Server 대신 → ConfigMap · Secret
                  Spring Cloud Kubernetes(ConfigMap → Environment 바인딩)만 쓴다.
                  별도 서버 프로세스를 띄우지 않는다.
```

**Kafka 연동에는 Spring Cloud Stream(Kafka Binder)을 쓴다.** 저수준
`KafkaTemplate`을 직접 감싸는 대신 표준 바인딩을 쓰면 컨슈머 그룹·리밸런싱
설정이 선언적으로 정리된다.

### 축소한 예산

```
k3s 컨트롤플레인      ~0.4GB
Spring Cloud Gateway ~0.4GB
서비스 3개           ~1.2~1.5GB
MySQL·Redis·Kafka    ~2.3~3.8GB   (ADR-012 로 MongoDB·ES는 오프로드/유예)
관측 스택            ~2~3GB
                    ─────────
                    ~6.3~9.1GB   → 12GB 안에 들어온다
```

## 근거

- **이미 있는 경계를 재사용한다.** 서비스를 나누기 위해 나누는 게 아니라
  `ADR-010`이 다른 이유(REST의 무상태성 보존)로 이미 갈라놓은 경계에
  오케스트레이션을 얹는 것이다
- **Eureka·Config Server를 빼는 것 자체가 판단력이다.** k8s 네이티브 기능과
  중복되는 것을 또 얹지 않는다는 근거를 댈 수 있다 — `flow.md` §18의
  *"기술을 사용하기 위해 도메인을 억지로 설계하지 않는다"*와 같은 원칙
- **단일 노드에서도 핵심 프리미티브는 배운다.** Deployment·Service·ConfigMap·
  Secret·rolling update — 멀티 노드가 아니어도 학습 목표는 채워진다

## 대가

- k3s 오버헤드(~0.3~0.5GB)가 예산에 새로 들어간다
- 배포 파이프라인이 바뀐다 — GitHub Actions가 `docker compose` 대신 k3s
  매니페스트(또는 Helm)를 적용해야 한다
- **단일 노드 k3s는 "노드 장애 시 자동 페일오버" 같은 K8s의 핵심 이점을 실제로
  못 얻는다.** 학습용이라는 것을 명확히 인지한다 — 이력서에 "고가용성 클러스터
  운영"이라고 쓰면 안 된다. "k3s로 MSA 배포·오케스트레이션을 구성했다"까지만
  쓸 수 있다
- 서비스 간 결합은 원래도 동기 호출이 아니었다(`ADR-010`이 Kafka만 경유하도록
  설계) — 그래서 네트워크 홉이 늘어도 새로운 실패 모드(타임아웃 연쇄)가
  추가되지는 않는다

## 재검토 조건

- **서비스를 3개보다 더 쪼개고 싶어질 때** → 그 전에 노드를 늘릴지부터
  결정한다. 예산이 지금도 빡빡하다
- Eureka·Config Server 없이 실제로 설정 관리가 불편해질 때 → Spring Cloud
  Kubernetes의 ConfigMap 바인딩으로 충분한지 재평가
- 트래픽이 늘어 멀티 노드가 실제로 필요해질 때 → 그때 노드를 추가한다
