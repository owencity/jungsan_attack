# ADR-002 · 비동기 알림에 메시지 브로커를 쓰지 않는다

**상태** 확정 · **관련** `SPEC.md` §10

## 맥락

비동기 처리가 필요한 지점은 **FCM 발송 하나뿐**이다. RabbitMQ 도입을 검토했다.

## 결정

**PostgreSQL에 아웃박스 테이블을 두고 `@Scheduled` 폴러가 발송한다.** 브로커를 도입하지 않는다.

```
notification_outbox
  id, type, payload(jsonb), status, retry_count,
  next_retry_at, created_at, sent_at
```

```kotlin
// 발행: 비즈니스 로직과 같은 트랜잭션
@Transactional
fun confirm(...) {
    gathering.confirm()
    outbox.save(...)      // 같은 커밋
}
```
```sql
-- 발송: 별도 스케줄러
SELECT * FROM notification_outbox
 WHERE status='PENDING' AND next_retry_at <= now()
 ORDER BY created_at LIMIT 100
 FOR UPDATE SKIP LOCKED;      -- 인스턴스가 늘어도 중복 발송 없음
```

## 근거

**브로커를 써도 아웃박스는 어차피 필요하다.** "DB 커밋"과 "메시지 발행"은 서로 다른 시스템이라
원자적으로 묶이지 않기 때문이다.

```
① DB에 입금확인 저장   ✓ 커밋됨
② RabbitMQ에 publish   ← 여기서 죽으면 알림이 영원히 안 가고, 아무도 모른다
```

아웃박스가 있으면 브로커가 필요 없어지고, 아웃박스가 없으면 브로커를 써도 유실이 난다.

| | 아웃박스 | RabbitMQ |
|---|---|---|
| 유실 방지 | 같은 트랜잭션 | 아웃박스를 추가해야만 |
| 재시도 | `retry_count` 컬럼 | DLQ 구성 필요 |
| 추가 인프라 | **없음** | 브로커 ~300MB+ |
| 발송 이력 조회 | `SELECT` | 별도 도구 |

12GB 단일 인스턴스에 앱·PostgreSQL·Loki·Prometheus·Grafana가 이미 올라간다.
**초당 몇 건 규모의 발송에 브로커 운영 비용을 얹을 이유가 없다.**

## 대가

- 폴링 주기만큼 발송이 지연된다 (수 초 ~ 수십 초). FCM 알림에는 문제되지 않는다
- 폴링 쿼리가 주기적으로 DB에 나간다. 인덱스 `(status, next_retry_at)`로 충분히 가볍다

## 재검토 조건

- **이벤트 소비자가 2개 이상**이 될 때 (예: 알림 + 통계 + 외부 연동)
- 발송 지연이 스케줄러 주기로 감당되지 않을 때
- 인스턴스가 여러 대로 늘어 폴링 경합이 실제 부하가 될 때

**아웃박스가 있으면 그때 뒤에 브로커를 붙이는 전환이 쉽다.** 없으면 그때 가서 만들어야 한다.
