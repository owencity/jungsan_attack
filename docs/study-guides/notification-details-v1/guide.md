# 내부 알림 보완 학습 방향표

https://github.com/owencity/jungsan_attack/pull/35

기준 `86e0801df2a06750a7d459ba02208a804fec4351` → `c7dff2f372b327f0a5b8dc69d28b2bae9240435f`. 정책·카탈로그는 원격 main `3eb525464a32a3415c4d3c0db9dd6bfd9bcc3420`로 고정한다. 학습 완료와 구현·배포는 별개다.

### Java 기본기

- 자바의 정석 14장 2.3 스트림의 중간연산 (p.958) — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java:10](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java#L10): distinct와 filter가 받는 사람의 중복·행동자 제외를 결정하는 새 파이프라인을 읽는다.

### Effective Java

- 해당 없음

### 함수형 Java

- Chapter 6 / Section 6.3, 스트림 파이프라인 구축하기 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java:11](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/java/app/jeongsan/server/gathering/javaimpl/NotificationPolicy.java#L11): DB 저장과 분리된 스트림의 중간 연산·최종 toList가 수신자 목록을 만드는 연결을 확인한다.

### Kotlin 기본

- 15강, 코틀린에서 배열과 컬렉션을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:231](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L231): 차수별 자동 응답을 참여자 Set으로 모으는 변경이 사용자별 알림 중복을 제거한다.

- 17강, 코틀린에서 람다를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:220](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L220): groupBy 람다와 참여자별 조회가 송금자 요약과 수취인만 있는 참여자 안내를 분리한다.

### Kotlin 고급

- 해당 없음

### 기술별 학습 포인트

#### JDBC 배치 조회

- val missingPayoutUsers= — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:242](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L242): IN 조회가 결제자 계좌 상태를 한 번에 모으므로 알림 생성에 N+1을 추가하지 않는다.

#### MySQL 트랜잭션·멱등

- with ThreadPoolExecutor(2) as pool: — [scripts/verify-notifications.py:85](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/scripts/verify-notifications.py#L85): 실제 최초 계좌 등록의 동시 두 요청이 사용자별 알림 하나로 남는지 SQL 잠금과 HTTP 결과를 함께 검사한다.

#### 단위 상태·삭제 계약

- val gatheringState=updateSummary(gid) — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:245](https://github.com/owencity/jungsan_attack/blob/c7dff2f372b327f0a5b8dc69d28b2bae9240435f/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L245): 단위 완료와 술자리 전체 완료를 구분한 상태로 삭제 예고를 정하므로 알림 내용과 실제 삭제 시점을 대조한다.
