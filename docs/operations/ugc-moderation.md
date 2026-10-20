# FC-024 신고 일일 처리 절차

운영자(CTO)는 **매일 최소 한 번**, 접수 후 24시간 안에 확인한다. 자동 스케줄·n8n 알림은 이번 결정에 없다.
신고 로그는 알림 보조이며 DB 미처리 조회가 기준이다. 사용자 메시지·이름·계좌를 채팅·CI·공개 로그에 복사하지 않는다.
운영 MySQL은 기존 SSH와 내부 docker compose mysql을 통해서만 접속한다. 비밀번호를 명령줄·문서에 넣지 않는다.
먼저 `SELECT DATABASE();`로 jeongsan 운영 DB인지 확인하고 세션을 UTC로 고정한다.

## 1. 미처리 조회·기한 점검

```sql
SET time_zone = '+00:00';
SELECT report.id, report.entry_id, entry.gathering_id, report.reason,
       report.created_at, entry.hidden_at,
       TIMESTAMPDIFF(HOUR, report.created_at, UTC_TIMESTAMP()) AS pending_hours
FROM message_reports report JOIN timeline_entries entry ON entry.id = report.entry_id
WHERE report.handled_at IS NULL ORDER BY report.created_at, report.id;
```

모든 미처리를 확인한다. 로그 조회는 `docker compose -f docker-compose.prod.yml logs --since 24h app`의
`MESSAGE_REPORTED entryId=… gatheringId=… reason=…`만 필터한다. 해당 로그에는 신고자·본문을 넣지 않는다.
본문 확인이 필요할 때만 비공개 운영 세션에서 다음 한 건을 조회한다.

```sql
SET @entry_id = 123; -- 실제 검토할 entry_id를 직접 지정
SELECT id, gathering_id, type, body, hidden_at FROM timeline_entries WHERE id = @entry_id;
```

## 2. 메시지 전체 숨김

신고가 2명이면 이미 숨겨져 있다. 1명이더라도 운영 검토로 숨길 수 있다.
자동 숨김·개인 신고 숨김은 handled_at을 채워도 풀리지 않는다.

```sql
START TRANSACTION;
SET @gathering_id = NULL;
SELECT gathering_id INTO @gathering_id FROM timeline_entries WHERE id = @entry_id;
SELECT id FROM gatherings WHERE id = @gathering_id FOR UPDATE;
UPDATE timeline_entries SET hidden_at = COALESCE(hidden_at, UTC_TIMESTAMP(6))
WHERE id = @entry_id AND type = 'MESSAGE';
SELECT ROW_COUNT() AS affected;
COMMIT;
```

## 3. 상습 작성자의 새 메시지 금지

계정·참여자·정산을 삭제하지 않는다. 과거 메시지는 별도 2번 조치로 숨긴다.
이후 POST messages는 403 MESSAGE_BANNED다. 차단·신고·정산·송금은 계속 사용할 수 있다.
대상 ID를 먼저 확인하고 사용자 행 잠금으로 메시지 전송과 직렬화한다.

```sql
SET @author_user_id = NULL;
SELECT participant.user_id INTO @author_user_id
FROM timeline_entries entry JOIN participants participant ON participant.id = entry.author_participant_id
WHERE entry.id = @entry_id AND entry.type = 'MESSAGE';
START TRANSACTION;
UPDATE users SET message_banned_at = COALESCE(message_banned_at, UTC_TIMESTAMP(6)) WHERE id = @author_user_id;
SELECT ROW_COUNT() AS affected;
COMMIT;
```

복구가 필요하면 별도 운영 판단과 비공개 작업 기록을 남긴 뒤
`UPDATE users SET message_banned_at=NULL WHERE id=@author_user_id;`로 새 메시지만 허용한다.
숨긴 메시지를 다시 노출하는 API는 이번 범위에 없다.

## 4. 처리 완료·재확인

어떤 조치를 했는지는 운영자의 비공개 작업 기록에 남긴다.
반드시 검토한 신고 ID만 지정하며 미처리 전체를 일괄 완료하지 않는다.

```sql
SET @report_id = 456; -- 검토를 마친 신고 번호
UPDATE message_reports SET handled_at = COALESCE(handled_at, UTC_TIMESTAMP(6)) WHERE id = @report_id;
SELECT id, entry_id, created_at, handled_at FROM message_reports WHERE id = @report_id;
SELECT COUNT(*) AS pending FROM message_reports WHERE handled_at IS NULL;
```

방의 7일/유휴 만료와 작성자 탈퇴 시 메시지·신고는 함께 삭제된다. 신고자 탈퇴는 ID만 NULL로 익명화한다.
차단은 사용자 탈퇴 시 양방향 FK CASCADE로 삭제된다. 신고 기록을 백업·다른 저장소에 무기한 복제하지 않는다.
앱 심사 녹화는 프론트·CTO가 실제 계정으로 신고→숨김·차단→숨김·차단 해제를 보여준다.
자동 API 테스트는 최종 앱 녹화를 대신하지 않는다.
