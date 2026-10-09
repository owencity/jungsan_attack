# backend-v4 PR 스터디 가이드

학습은 개발 완료 후에도 이 버전에서 이어간다. 원격 main 목차·정책만 사용했고 추천 줄은 모두 실제 추가/변경 diff 줄인지 검증했다.

- source base: `6269774cade6e032c762cdd568545246b6331002`
- source head: `be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb`
- 목차·정책 source: `origin/main@d5feb4677f71c9fe973e31118fd3b82d27eb2788`
- [고정 diff](https://github.com/owencity/jungsan_attack/compare/6269774cade6e032c762cdd568545246b6331002...be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb) · [오프라인 diff](changes.patch) · [목차 원본](catalog.md) · [리뷰 체크](review.md) · [진도](progress.md) · [n8n 형식 JSON](n8n-format.json)

Core/단위 규칙/계좌 암호화는 Java/Kotlin 독립 구현이다. Spring HTTP·JDBC 배선까지 전부 두 언어로 작성한 상태는 아니며 공유 인프라로 표시한다.

## Java 기본기

- **JAVA-CONSTRUCTOR 생성자; JST-06-05-01 생성자란? (6장 객체지향 프로그래밍 I / 5. 생성자(Constructor) / p.315)** → [SettlementWorkflow 생성자 · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:12](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L12) — private 생성자가 계산용 객체 생성을 막는 지점을 diff에서 확인한다.
- **JST-07-04-03 final - 마지막의, 변경될 수 없는 (7장 객체지향 프로그래밍 II / 4. 제어자(modifier) / p.369)** → [SettlementWorkflow 타입 · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:11](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L11) — 상속을 막는 final과 private 생성자의 서로 다른 역할을 코드에서 구분한다.
- **JST-11-01-10 HashMap과 Hashtable (11장 컬렉션 프레임웍 / 1. 컬렉션 프레임웍(collections framework) / p.674)** → [input · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:48](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L48) — AttendanceKey를 키로 삼아 각 사람×차수 응답을 한 번씩 넣는 관점으로 본다.
- **JST-14-01-06 메서드 참조 (14장 람다와 스트림 / 1. 람다식(Lambda expression) / p.946); JST-14-02-03 스트림의 중간연산 (14장 람다와 스트림 / 2. 스트림(stream) / p.958)** → [input · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:45](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L45) — filter→sorted→toList에서 다른 단위 사람과 차수가 어느 단계에 제거되는지 추적한다.

## Effective Java

- **EJ-04 인스턴스화를 막으려거든 private 생성자를 사용하라** → [SettlementWorkflow 생성자 · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:12](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L12) — 인스턴스 상태 없는 static 기능 묶음의 생성 방지 의도가 실제 구조와 맞는지 본다.
- **EJ-50 적시에 방어적 복사본을 만들라** → [UnitInput compact constructor · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:14](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L14) — record 필드가 final이어도 외부 List 변경을 별도로 막아야 하는 지점을 본다.
- **EJ-49 매개변수가 유효한지 검사하라** → [payout · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:104](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L104) — 은행·숫자·예금주를 암호화 전에 거절하는 인수 검증을 본다.

## 함수형 Java

- **FJ-05-02 도움을 주기 위한 레코드 (§5.2)** → [UnitInput · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:13](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L13) — Java record의 입력 묶음과 Kotlin UnitInput의 data class를 같은 필드 기준으로 비교한다.
- **FJ-06-03 스트림 파이프라인 구축하기 (§6.3)** → [input · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:46](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L46) — 파이프라인 중 unitId 필터를 빼면 B의 실패가 A에 섞이는지 diff로 확인한다.
- **FJ-04-04 불변성 만들기 (§4.4)** → [RoundInput compact constructor · server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java:19](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/java/app/jeongsan/server/gathering/javaimpl/SettlementWorkflow.java#L19) — 외부 술 항목 목록의 변경이 다음 계산에 들어오는지를 방어 복사 테스트와 대조한다.

## Kotlin 기본

- **KB-03 코틀린에서 Type을 다루는 방법 (섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법 / 3강); KB-05 코틀린에서 제어문을 다루는 방법 (섹션 3. 코틀린에서 코드를 제어하는 방법 / 5강)** → [preview · server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt:74](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt#L74) — when의 is 분기 뒤 outcome.result 접근을 Java의 instanceof 분기와 비교한다.
- **KB-14 코틀린에서 다양한 클래스를 다루는 방법 (섹션 4. 코틀린에서의 OOP / 14강)** → [UnitInput · server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt:9](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt#L9) — DB 행 전체 대신 계산 단위의 id·명단·상태·revision만 옮기는 타입 경계를 본다.
- **KB-12 코틀린에서 object 키워드를 다루는 방법 (섹션 4. 코틀린에서의 OOP / 12강)** → [SettlementWorkflow · server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt:20](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt#L20) — Kotlin object와 Java final/private 생성자/static 묶음을 실제 호출 방식으로 비교한다.
- **KB-18 코틀린에서 컬렉션을 함수형으로 다루는 방법 (섹션 5. 코틀린에서의 FP / 18강)** → [input · server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt:49](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt#L49) — 명단×차수 조합과 미응답 DRANK 투영을 만드는 flatMap/toMap을 추적한다.
- **KB-16 코틀린에서 다양한 함수를 다루는 방법 (섹션 5. 코틀린에서의 FP / 16강)** → [Map.instant 확장 함수 · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringStore.kt:29](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringStore.kt#L29) — 확장 함수가 DB Map 타입에 붙인 호출 문법과 실제 Timestamp/LocalDateTime 변환을 본다.
- **KB-20 코틀린의 scope function (섹션 6. 추가적으로 알아두어야 할 코틀린 특성 / 20강)** → [authorize / 호출부 · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:37](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L37) — also로 인가·OPEN 검사를 수행하고 원래 UnitInput을 반환하는 흐름을 본다.

## Kotlin 고급

- **KA-20 코틀린의 어노테이션 (섹션 6. 어노테이션과 리플렉션 / 20강)** → [RoundRequest · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringDto.kt:11](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringDto.kt#L11) — Kotlin 생성자 프로퍼티의 field 대상 어노테이션이 실제 Bean Validation에 적용되는지를 본다.

## 기술별 학습 포인트

- **MySQL FK 공유 잠금 뒤 FOR UPDATE 승격과 unit→Gathering 선행 배타 잠금의 교착 차이를 본다** → [settle / room · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:173](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L173) — MySQL FK 공유 잠금 뒤 FOR UPDATE 승격과 unit→Gathering 선행 배타 잠금의 교착 차이를 본다.
- **preview 후 다른 응답이 들어오면 revision/hash 거절과 트랜잭션 rollback이 어디서 보장되는지 본다** → [settle · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:172](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L172) — preview 후 다른 응답이 들어오면 revision/hash 거절과 트랜잭션 rollback이 어디서 보장되는지 본다.
- **WAITING이어도 sentAt 이력이 있으면 취소할 수 없는 금융 상태와 DB 상태를 대조한다** → [reopen / transfer · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:190](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L190) — WAITING이어도 sentAt 이력이 있으면 취소할 수 없는 금융 상태와 DB 상태를 대조한다.
- **단위 전체 잠금→방 잠금→새 단위·활동 재검사가 개인정보 삭제와 동시 요청에 어떤 경계를 주는지 본다** → [deleteExpired · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:276](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt#L276) — 단위 전체 잠금→방 잠금→새 단위·활동 재검사가 개인정보 삭제와 동시 요청에 어떤 경계를 주는지 본다.
- **같은 DataSource의 Spring 트랜잭션 참여와 테이블별 IN 조회를 롤백·목록 테스트에서 확인한다** → [GatheringStore / details · server/src/main/kotlin/app/jeongsan/server/gathering/GatheringStore.kt:4](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/GatheringStore.kt#L4) — 같은 DataSource의 Spring 트랜잭션 참여와 테이블별 IN 조회를 롤백·목록 테스트에서 확인한다.
- **AES-GCM nonce가 매번 달라지고 변조를 거절하는지 Java/Kotlin 상호 복호화 테스트와 대조한다** → [PayoutCipher.encrypt · server/src/main/kotlin/app/jeongsan/server/gathering/PayoutCipher.kt:8](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/PayoutCipher.kt#L8) — AES-GCM nonce가 매번 달라지고 변조를 거절하는지 Java/Kotlin 상호 복호화 테스트와 대조한다.
- **필드 구분자·개수·길이 접두사와 안정된 정렬이 같은 입력을 같은 hash로 만드는지 본다** → [hash · server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt:58](https://github.com/owencity/jungsan_attack/blob/be7d4ee1a68c9eae5e095fa4d283c0923ae63fcb/server/src/main/kotlin/app/jeongsan/server/gathering/SettlementWorkflow.kt#L58) — 필드 구분자·개수·길이 접두사와 안정된 정렬이 같은 입력을 같은 hash로 만드는지 본다.

