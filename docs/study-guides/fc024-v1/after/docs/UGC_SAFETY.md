# FC-024 메시지 안전 정책 · API v13

CTO 2026-10-20 결정: 메시지 기능을 유지하며 신고·차단·금칙어·운영 SQL을 제공한다.
신고 알림은 서버 로그와 운영자의 일일 조회(ⓐ)다. n8n·메일·약관 서버 저장은 이번 범위에 없다.

## API 계약

모두 기존 WEB 쿠키 / APP Bearer 인증을 사용한다. `/api/v1` 프리픽스를 생략하지 않는다.

| 요청 | 응답·규칙 |
|---|---|
| POST /api/v1/gatherings/{gid}/messages/{entryId}/reports `{reason}` | 204. ACTIVE 참여자만, 그 방의 MESSAGE만, 본인 글 불가. reason은 ABUSE / SEXUAL / SPAM / OTHER. 같은 사람·메시지는 첫 사유를 유지하고 멱등 204 |
| POST /api/v1/me/blocks `{gatheringId,participantId}` | 204. 본인과 대상이 해당 술자리의 ACTIVE 참여자여야 한다. 탈퇴 계정·본인 대상은 400. 같은 사용자 재차단은 최초 시각 유지, 멱등 204 |
| GET /api/v1/me/blocks | 200 `[{userId,displayName,blockedAt}]`, blockedAt 내림차순·userId 오름차순. 본인 목록만 |
| DELETE /api/v1/me/blocks/{userId} | 204. 본인이 건 차단만 삭제. 이미 없거나 탈퇴한 대상도 멱등 204 |

신고·차단의 잘못된 종류·사유·본인 대상은 400 MALFORMED_REQUEST, 없는 방·메시지는 404 NOT_FOUND,
비참여자는 403 NOT_PARTICIPANT다. 잘못된 본문 검증은 기존 400 VALIDATION_FAILED/MALFORMED_REQUEST를 따른다.

GET 술자리 상세·목록의 timeline은 신고자 본인의 신고 MESSAGE와 본인이 차단한 사용자의 MESSAGE를 제외한다.
서로 다른 신고자 2명부터 hidden_at을 한 번 설정하여 작성자를 포함한 모두에게 숨긴다.
처리 완료(handled_at)는 운영 상태일 뿐, 개인 숨김·전체 숨김을 해제하지 않는다.
SYSTEM·SPOON, 참여자·응답·명단·정산·송금과 권한에는 영향이 없다. 차단은 단방향이다.

기존 POST messages는 201 응답 모양을 유지한다. message_banned_at이 있으면 403 MESSAGE_BANNED
`메시지를 보낼 수 없는 계정이에요`, 금칙어면 422 MESSAGE_NOT_ALLOWED `부적절한 표현이 있어요`이며 저장하지 않는다.
금칙어는 리소스 기본 목록을 NFKC·소문자·문자/숫자만 남기는 정규화 후 포함 비교한다.
단어 뜻·문맥을 판별하지 못해 오탐·신조어 누락이 있을 수 있다. 금칙어 추가는 리소스 변경 PR로 관리한다.

## 저장·동시성·삭제

진실은 신규 019-ugc-safety.yaml / changeSet 029다. 기존 changelog는 변경하지 않는다.
- message_reports: id, entry_id, reporter_user_id, reason, created_at, handled_at. UNIQUE(entry_id,reporter_user_id).
  entry FK CASCADE로 방 만료·작성자 탈퇴 시 메시지와 함께 지운다. 신고자 탈퇴는 reporter_user_id를 NULL로 익명화한다.
  각 행은 원래 서로 다른 신고자가 만든 것이므로 익명화 뒤에도 접수한 신고 수와 hidden_at은 유지한다. 본문·계좌·이름 스냅샷은 복제하지 않는다.
- user_blocks: PK(blocker_user_id,blocked_user_id), created_at. 어느 계정이든 탈퇴하면 FK CASCADE로 제거한다.
- timeline_entries.hidden_at, users.message_banned_at: nullable UTC DATETIME(6). 기존 데이터의 동작은 NULL로 유지한다.

신고는 기존 삭제 순서와 같은 Gathering 잠금 뒤 메시지 행을 잠가 중복·2명 경합을 직렬화한다.
차단은 Gathering 잠금 뒤 사용자 행을 id 순서로 공유 잠금해 탈퇴와 경합을 제어하고 PK로 중복을 막는다.
메시지는 기존 Gathering 잠금 뒤 작성자 공유 잠금으로 메시지 금지 SQL과 직렬화한다.
조회는 한 timeline 배치 쿼리의 NOT EXISTS로 숨김을 적용해 방·항목마다 쿼리를 추가하지 않는다.

## 신고 로그·운영

신규 신고가 커밋된 뒤에만 한 줄 `MESSAGE_REPORTED entryId=… gatheringId=… reason=…`를 남긴다.
중복 요청은 새 신고·새 로그를 만들지 않는다. 로그에 본문·사용자 ID·이름·계좌·토큰은 넣지 않는다.
커밋 후 프로세스 종료 등으로 로그가 유실될 수 있으므로 DB의 미처리 신고가 운영 확인의 기준이다.
운영자는 매일 최소 한 번 [신고 처리 SQL 절차](operations/ugc-moderation.md)를 실행하고 24시간 안에 확인한다.
코드는 운영자의 실제 일일 확인이나 프론트 약관 동의·App Store 재심사 통과를 증명하지 않는다.
