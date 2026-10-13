# 최초 실제 DB 검증 실패

검증 head `9aa173e08fb65ada05b80f3a26fff011bf7229fe`, run 38427169635.
Core·서버 테스트/빌드는 통과했고 기존 자동 정산/UTC 20건 뒤 신규 알림 테스트의 최초 사용자 INSERT에서 실패했다.
테스트 제공자 NOTIFY_TEST는 11자이며 적용된 users.provider는 VARCHAR(10)이다. fixture를 10자 NOTIF_TEST로 바꾼다.
스키마 제약이나 업무 코드를 바꾸거나 테스트를 건너뛰지 않는다. 최초 실패를 보존하고 최신 head의 전체 실제 DB/HTTP 검증을 다시 실행한다.
