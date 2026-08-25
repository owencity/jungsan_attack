# ADR-010 · 실시간 채팅은 Netty + WebSocket + MongoDB, REST 서버와 분리한다

**상태** ⏸️ **부분 보류 — MVP는 같은 프로세스에 얹는다** · **관련** `ADR-014`, `ADR-002`, `ADR-009`, `ADR-012`, `report/flow.md`

> **`ADR-014`에 의해 조정됨(2026-08-23).** MVP 단계에서는 채팅을 **별도 프로세스로
> 빼지 않고** 기존 Spring Boot 프로세스 안에 `spring-boot-starter-websocket`으로
> 얹는다(`server/chat/` 패키지). Tomcat NIO 기준 동시 접속 수십~수백 명 규모에서는
> 아래 "커넥션 풀·스레드 모델 간섭" 우려가 체감되지 않기 때문이다.
>
> **아래 설계(분리, Netty, MongoDB, Kafka 경유)는 그대로 유효하며 보류 상태다.**
> `ADR-014`의 재검토 조건(재배포로 채팅이 끊기는 게 실제 불편이 될 때, 트래픽을
> 따로 스케일해야 할 때)이 오면 이 문서대로 분리한다. 메시지 저장도 MVP에서는
> MySQL로 시작하고, 이력이 감당 안 될 때 MongoDB로 옮긴다.

## 맥락

`report/flow.md`가 `Gathering` 진행 중 실시간 모임방을 요구한다 — 채팅, 입장/퇴장,
현재 접속자 표시, 지출 이벤트가 채팅에 함께 뜨는 것("민수님이 지출을 등록했습니다").

기존 Spring REST 서버(`ADR-006`)는 **짧은 요청-응답**을 위해 설계됐다. WebSocket
연결은 **오래 유지되는 상태 저장(stateful) 연결**이라 성격이 다르다 — REST 서버에
그대로 얹으면 커넥션 풀·스레드 모델이 서로를 간섭한다.

## 결정

**Netty 기반 WebSocket 게이트웨이를 REST API 서버와 별도 프로세스로 둔다.**

```
Client (참여자 웹 · 총무 앱)
  │  WebSocket
  ▼
Netty 게이트웨이
  │             │              │
  ▼             ▼              ▼
MongoDB       Redis          Kafka
(메시지 저장)   (접속자·읽음위치)  (지출 이벤트 구독)
```

### 메시지 저장 — MongoDB (`flow.md` 스키마 그대로)

```json
{
  "groupId": 100, "meetingId": 200, "memberId": 7,
  "type": "TEXT",
  "message": "2차 어디감?",
  "createdAt": "..."
}
```

`type`: `TEXT | IMAGE | EXPENSE | SETTLEMENT | JOIN | LEAVE | NOTICE`.
**MongoDB는 자체 호스팅하지 않고 Atlas 무료 티어로 오프로드한다** (`ADR-012`).
스키마리스 특성이 이 타입 확장에 맞다 — 새 타입이 추가돼도 마이그레이션이 없다.

### 접속 상태 — Redis (자체 호스팅)

```
meeting:{id}:online                    현재 접속자 집합
chat:last-read:{groupId}:{memberId}    마지막 읽음 위치
```

`Redis`가 이미 `ADR-008`의 정원 잠금 카운터 후보로 검토됐던 것과 별개로,
여기서는 **연결 상태**(휘발성)를 다룬다. 영구 데이터 저장소로 쓰지 않는다
(`flow.md` §18 원칙 — 여기서는 이미 지키고 있던 것).

### 지출 이벤트가 채팅에 뜨는 경로

```
Round/ExtraItem 저장 (REST 서버, MySQL 트랜잭션)
  → Outbox 테이블에 RoundCreated 적재 (같은 트랜잭션)
  → @Scheduled 폴러가 Kafka 로 발행 (ADR-002 확장 — 아래 참조)
  → Netty 게이트웨이가 그 토픽을 구독
  → 해당 meetingId 의 WebSocket 세션에 EXPENSE 타입 메시지로 브로드캐스트
  → MongoDB 에도 시스템 메시지로 남긴다 (채팅 이력에 함께 보이도록)
```

**REST 서버가 WebSocket 세션을 직접 알 필요가 없다.** Kafka 를 경유하므로
두 서버가 완전히 분리된 채로 동작한다 — REST 서버가 죽어도 이미 연결된
채팅은 살아있고, 채팅 게이트웨이가 죽어도 정산 트랜잭션은 영향받지 않는다.

### 인증 — WebSocket 핸드셰이크에서 JWT 검증

```
GET /ws/meetings/{id}?token=<JWT>     ← 쿼리 파라미터로 전달
  또는
Sec-WebSocket-Protocol: bearer.<JWT>  ← 서브프로토콜로 전달 (권장)
```

**쿼리 파라미터는 서버 접근 로그·프록시 로그에 토큰이 남는다.** `SPEC.md` §10의
"로그에 shareToken을 남기지 말 것"과 같은 이유로, **서브프로토콜 방식을 우선한다.**
브라우저가 커스텀 헤더로 WebSocket 핸드셰이크를 못 보내는 제약(fetch와 달리
`new WebSocket()`은 임의 헤더 추가가 불가) 때문에 이 우회가 필요하다.

핸드셰이크에서 검증한 뒤에는 세션에 `userId`를 붙여두고, 메시지마다 다시
검증하지 않는다 — REST 의 매 요청 인증과 다른 지점이다.

## 근거

- **REST 의 무상태성이 깨지지 않는다.** 배포·스케일 단위가 분리된다 — REST 서버를
  재배포해도 진행 중인 채팅 연결이 끊기지 않는다
- **MongoDB 스키마리스가 메시지 타입 확장에 맞다.** `TEXT`/`EXPENSE`/`SETTLEMENT`가
  전혀 다른 필드를 가져도 마이그레이션 없이 얹을 수 있다
- **Kafka 를 경유해 REST-채팅 결합을 없앤다.** REST 서버가 WebSocket 세션 목록을
  들고 있을 필요가 없다 — `ADR-002`가 확장하는 그 경로를 그대로 재사용한다

## 대가

- **프로세스가 하나 더 늘어난다.** 12GB 예산에 Netty 게이트웨이 몫(~0.3~0.5GB
  추정, 실측 필요)을 반영해야 한다
- **WebSocket 재연결 처리를 프론트가 직접 구현해야 한다.** REST 는 요청이 실패하면
  재시도만 하면 되지만, WebSocket 은 끊긴 동안의 메시지를 놓치지 않는 로직이 별도로
  필요하다 — 재연결 시 `last-read` 이후 메시지를 MongoDB 에서 다시 읽어온다
- **트랜잭션 경계가 REST 서버와 채팅 게이트웨이 사이에서 끊긴다.** 지출이 저장됐는데
  Kafka 발행이 지연되면, 채팅방에는 몇 초 늦게 뜬다 — **이건 허용한다.** 정산 금액의
  정확성(`ADR-005`)과 채팅 알림의 실시간성은 다른 요구 수준이다

## 재검토 조건

- 트래픽이 늘어 Netty 게이트웨이가 별도 인스턴스로 분리돼야 할 때
- 재연결 시 메시지 유실이 실제로 보고될 때 → 클라이언트 측 시퀀스 번호 도입 검토
