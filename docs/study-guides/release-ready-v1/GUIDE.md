# 출시 PR 스터디 가이드 · release-ready-v1

- 실제 PR 범위: main `f094e884580159ffb82844bd3d759cf028c96458` → 코드 `5fd757e7c14c8099021349a142d9500199d0453c`.
- 이번 추가 구현 범위: `cbd091d7e5e4a2135e95f69d878361bf962edab9` → `5fd757e7c14c8099021349a142d9500199d0453c`. 아래 항목은 추가 구현의 변경 줄을 실제 PR diff에서도 확인했다.
- 목차·정책: origin/main `f094e884580159ffb82844bd3d759cf028c96458`의 저장 문서. 책 본문을 읽었다거나 스터디를 완료했다고 표시하지 않는다.
- 기존 실명·Core Java/Kotlin·독립 단위·인증·CI 학습은 해당 고정 가이드 및 PR #26의 전체 인덱스에서 이어 본다.
- 읽을 순서: PR diff의 지정 줄 → 그 함수 전체 → 아래 목차 위치 → 왜 이렇게 짰는지 스스로 리뷰.

### Java 기본기

- `JAVA-COLLECTION` 컬렉션 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:35 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L35): 응답 키를 Set으로 모아 같은 참여자·차수 값으로 응답 완료를 찾는 관점.
- `JST-11-01-07` 자바의 정석 11장 1.7 Comparator와 Comparable (p.658) — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:29 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L29): 총무 우선·가입 시각·id 동률 순서가 최종 계산 대상자를 결정하는 관점.
- `JST-14-01-06` 자바의 정석 14장 1.6 메서드 참조 (p.946) — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:30 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L30): 메서드 참조가 비교할 값을 전달하고 thenComparing이 동률을 처리하는 관점.
- `JST-14-02-03` 자바의 정석 14장 2.3 스트림의 중간연산 (p.958) — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:28 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L28): filter·sorted·map으로 명단 선택과 정렬을 조립하는 관점.
- `JST-14-02-06` 자바의 정석 14장 2.6 collect() (p.980) — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:36 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L36): collect의 결과가 응답 완료 판정용 Set이 되는 관점.

### Effective Java

- `EJ-14` Item 14, Comparable을 구현할지 고려하라 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:29 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L29): 동일 가입 시각에서도 id로 결정되어 재시도마다 명단이 바뀌지 않는 관점.
- `EJ-17` Item 17, 변경 가능성을 최소화하라 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:34 · AutoSettlementPolicy.plan / Plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L34): record 프로퍼티와 결과 목록의 변경 가능성을 각각 확인하는 관점.
- `EJ-49` Item 49, 매개변수가 유효한지 검사하라 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:19 · AutoSettlementPolicy.validateHeadcount](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L19): HTTP 검증을 우회한 직접 호출에도 인원 범위를 검사하는 관점.

### 함수형 Java

- `FJ-02-02` Chapter 2 / Section 2.2, 람다의 실전 활용 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:31 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L31): 람다·메서드 참조가 실제 명단 필터와 ID 추출에 연결되는 관점.
- `FJ-15-03` Chapter 15 / Section 15.3, 명령형 세계의 함수형 아키텍처 — [server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:24 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java#L24): 판정은 입력으로만 계산하고 DB 잠금·알림·확정은 서비스에서 처리하는 경계 관점.

### Kotlin 기본

- `KB-02` 2강, 코틀린에서 null을 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:263 · GatheringService.autoSettle](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L263): 미입력 인원 null이 자동 정산을 끄는 조건이며 숫자 0과 구분되는 관점.
- `KB-07` 7강, 코틀린에서 예외를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:274 · GatheringService.autoSettle](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L274): Core 검증 오류만 보류하고 DB 실패는 전파되어 전체 트랜잭션이 취소되는 관점.
- `KB-14` 14강, 코틀린에서 다양한 클래스를 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/AutoSettlementPolicy.kt:6 · AutoSettlementPlan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/AutoSettlementPolicy.kt#L6): data class 결과의 includedIds·excludedIds·error와 Java Plan record를 대조하는 관점.
- `KB-18` 18강, 코틀린에서 컬렉션을 함수형으로 다루는 방법 — [server/src/main/kotlin/app/jeongsan/server/gathering/AutoSettlementPolicy.kt:19 · AutoSettlementPolicy.plan](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/AutoSettlementPolicy.kt#L19): filter·정렬·take/drop이 인원 안·밖을 만들고 Java Stream과 같은 순서인지 보는 관점.
- `KB-19` 19강, 코틀린의 이모저모 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:160 · GatheringService.respond](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L160): return@forEach가 현재 응답 칸만 건너뛰며 함수 전체 반환과 다른 관점.

### Kotlin 고급

- `KA-20` 20강, 코틀린의 어노테이션 — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringDto.kt:8 · HeadcountRequest](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringDto.kt#L8): @field:Min/Max가 Bean Validation이 읽는 JVM 필드에 붙어 HTTP 범위를 검증하는 관점.

### 기술별 학습 포인트

- GatheringService.autoSettle / unit — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:260 · GatheringService.autoSettle / unit](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L260): 단위 FOR UPDATE 잠금과 settlements UNIQUE가 마지막 응답 경합에서 1회 확정을 보장하는 관점.
- GatheringService.autoSettle / persistSettlement — [server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:281 · GatheringService.autoSettle / persistSettlement](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L281): Core 성공 뒤에만 제외·스냅샷·알림을 쓰고 DB 실패 때 함께 rollback하는 관점.
- spring.datasource.url — [server/src/main/resources/application-local.yml:9 · spring.datasource.url](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/resources/application-local.yml#L9): JVM 시간대·JDBC 변환·DB 세션 UTC·JSON Z 표기를 각각 비교하는 관점.
- Liquibase changeSet 028 — [server/src/main/resources/db/changelog/018-auto-settlement.yaml:3 · Liquibase changeSet 028](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/server/src/main/resources/db/changelog/018-auto-settlement.yaml#L3): 기존 checksum을 바꾸지 않고 nullable 인원 컬럼을 추가해 기존 단위의 동작을 유지하는 관점.
- health.sh — [scripts/deploy/health.sh:13 · health.sh](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/scripts/deploy/health.sh#L13): 다른 서비스의 HTTP UP과 실제 배포 이미지·컨테이너·포트의 일치를 구분하는 관점.
- deploy.sh — [scripts/deploy/deploy.sh:10 · deploy.sh](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/scripts/deploy/deploy.sh#L10): 성공 SHA 기록·실패 이미지 복원과 되돌리지 않는 DB migration의 호환성을 점검하는 관점.
- Backend CI 실제 DB 검사 — [.github/workflows/ci.yml:46 · Backend CI 실제 DB 검사](https://github.com/owencity/jungsan_attack/blob/5fd757e7c14c8099021349a142d9500199d0453c/.github/workflows/ci.yml#L46): MockMvc·순수 함수 검사와 실제 DB/HTTP 경합 시험의 범위를 구분하는 관점.
