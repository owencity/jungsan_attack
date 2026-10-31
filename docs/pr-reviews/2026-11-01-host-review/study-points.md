# FC-029 PR 리뷰·학습 후보

학습 상태 **UNRECORDED**. 코드 범위는 self-review.md의 고정 base/head와 changes.patch.
목차 근거: origin/main `8d27496270e6c8adcb78b4580d68c150f9c3cce6`의 STUDY_POLICY.md·STUDY_CATALOG.md.
책 본문을 읽거나 외부 리뷰를 완료했다는 의미가 아니다.

- Java 기본기 · JAVA-COLLECTION · 컬렉션: `server/src/main/java/app/jeongsan/server/gathering/javaimpl/AutoSettlementPolicy.java:34`, plan. subList(0, headcount)에서 List.copyOf(ordered)로 바뀌며 포함 대상과 외부 변경 가능성이 어떻게 달라지는지 본다.
- Effective Java · EJ-76 · 10장 예외 / 아이템 76 ‘가능한 한 실패 원자적으로 만들라’: `server/src/main/kotlin/app/jeongsan/server/gathering/GatheringService.kt:439`, refreshCalculationError. 계산 검증 실패는 응답 제출을 롤백하지 않되 확정 쓰기는 별도 트랜잭션 경로에서 원자적으로 수행한다. 도메인 실패와 인프라 실패를 구분한다.
- 기술별 학습 포인트 · UNMATCHED · 상태 전이와 부수효과: GatheringService:361/364/439. 입력 저장과 계산 조회를 금액 확정에서 분리하고, 스냅샷 저장 함수의 호출 경로가 총무 명령 하나로 좁아졌는지 확인한다.
- 기술별 학습 포인트 · UNMATCHED · 낙관적 입력 검증과 비관적 행 잠금: `scripts/verify-auto-settlement.py:79` 이후. 제외 전 해시는 409로 거절하고 동시 확정은 한 번만 성공한다. revision/hash는 오래된 화면을 탐지하고 DB 잠금은 동시에 실행되는 확정끼리 직렬화한다. JVM synchronized와 혼동하지 않는다.

리뷰 순서: FC-029의 사용자 계약 → 자동 제외/확정 호출 제거 diff → Kotlin/Java 전체 명단 판정 → 실제 DB 검증 → 웹/앱 버튼과 경고 대조.
