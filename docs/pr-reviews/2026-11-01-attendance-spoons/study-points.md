# 고정 PR diff 학습 후보

기준: ec4a155206ca24588f477ea2ce7bed257ab37897 → 56087f03a5b08ea1fdcbb92fcc6ca4a2548fd506.
카탈로그/정책: origin/main 8d27496. 상태 UNRECORDED. 책 본문을 읽었다는 뜻이 아니다.

- Effective Java · EJ-04 · Item 4 ‘인스턴스화를 막으려거든 private 생성자를 사용하라’: `server/src/main/java/app/jeongsan/server/gathering/javaimpl/AttendanceSpoonPolicy.java:5`에서 상태 없는 Java 정책 클래스를 인스턴스화하지 않는 근거를 본다.
- Effective Java · EJ-76 · 10장 예외 / Item 76 ‘가능한 한 실패 원자적으로 만들라’: `server/src/main/kotlin/app/jeongsan/server/gathering/AttendanceSpoonService.kt:41`의 이력 저장 뒤 잔액 갱신이 실패해도 부분 지급이 남지 않아야 하는 이유를 DB 롤백 검증과 연결한다.
- Kotlin 기본 · KB-02 · 2강 ‘코틀린에서 null을 다루는 방법’: `AttendanceSpoonService.kt:19`의 nullable JDBC 계정 값과 안전 캐스트·엘비스 반환이 탈퇴한 총무에게 지급하지 않는 흐름을 만든다.
- Kotlin 기본 · KB-12 · 12강 ‘코틀린에서 object 키워드를 다루는 방법’: `AttendanceSpoonPolicy.kt:4`의 상태 없는 object 정책과 Java private 생성자/static 메서드의 대응을 비교한다.
- 기술별 · UNMATCHED · Spring 트랜잭션 전파: `AttendanceSpoonService.kt:11`의 MANDATORY가 응답 저장 트랜잭션 없이 지급 서비스만 호출되는 일을 어떻게 막는지 본다.
- 기술별 · UNMATCHED · MySQL 멱등성과 유일 제약: `server/src/main/resources/db/changelog/022-attendance-spoons.yaml:16`의 복합 PK가 같은 술자리/총무/참여자의 지급 사건을 어떻게 식별하는지 본다.
- 기술별 · UNMATCHED · MySQL 행 잠금과 원자 증가: `AttendanceSpoonService.kt:33` 및 `:45`의 FOR UPDATE와 상대 증가가 각각 중복 판정과 오래된 잔액 덮어쓰기를 어떻게 다루는지 본다.
- 기술별 · UNMATCHED · FK 잠금·교착: `GatheringService.kt:346`의 사용자 ID 순 잠금이 두 총무의 교차 가입과 FK 공유 잠금 승격에 필요한 이유를 실제 HTTP 경합 테스트와 연결한다.

경로 축약은 모두 `server/src/main/kotlin/app/jeongsan/server/gathering/` 기준이다. 설명·언어 학습 후보는 위 고정 diff의 변경 줄만 근거로 삼았다.
