# FC-024 자체 리뷰

기준 코드 b71d7766558dc402355f6ec1790b87756ebbbf93. 자동/외부 리뷰·사용자 학습과 별개인 Codex 자체 점검이다.

- 권한: 기존 LoginUser 유지. ACTIVE 참여자·해당 방 MESSAGE·본인 제외를 서버에서 검증한다. 차단 대상은 participantId에서 사용자로 찾는다.
- 멱등: 신고 UNIQUE(entry,user), 차단 복합 PK. 첫 사유·시각을 유지하며 중복 접수는 새 로그를 만들지 않는다.
- 동시성: Gathering→사용자→메시지 순서로 기존 탈퇴·만료와 맞춘다. 메시지 금지 SQL과 작성자의 공유 잠금을 대조했다.
- 원자성: 신고·숨김은 한 트랜잭션, 로그는 AFTER_COMMIT. 금칙어·금지 실패는 INSERT 전이다.
- 정보 노출: 로그는 entryId·gatheringId·검증된 reason뿐이다. 차단 상대에 알림 없음. 신고 본문·계좌 스냅샷 없음.
- 조회: timeline 기존 IN 배치 한 번의 NOT EXISTS, 관련 인덱스와 FK 제공. 명단·금액·송금 조립은 변경하지 않았다.
- 호환: 기존 Liquibase 내용·번호 보존, nullable 열 신규, prod 설정·의존성 변경 없음. 계정 탈퇴는 SET NULL/CASCADE로 처리한다.
- 정책: Java/Kotlin 독립 구현과 기대값 테스트를 함께 둔다. 단순 필터 오탐·신조어와 실제 운영자 일일 실행은 남는 한계다.
- 운영 SQL: SELECT INTO 대상 변수를 NULL로 초기화해 없는 entryId에서 이전 사용자를 금지하지 않도록 했다.
- 검증: 로컬 Core54/Server170 통과, 실패/오류/건너뛰기0. 조건부 테스트의 Kotest 리스너 경고 후 전체 서버를 다시 실행해 성공했다.
- 실제 DB/HTTP29·Core54/서버170·기존 회귀 전체가 CI39419625606에서 성공했다(ci-source.json). 초기 두 검증 실패와 보완은 ci-attempts.json에 남긴다. 운영204는 배포 후속이며 PR에 결과를 별도로 기록한다.
