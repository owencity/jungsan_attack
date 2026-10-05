# Core v2의 Java·Kotlin 구현

같은 계산 규칙을 두 언어에서 각각 구현한다. Java는 Kotlin 계산기를 호출하지 않으며,
서버의 기존 Kotlin 진입점 `Settlement.settle(input)`을 유지한다. 면제 계산은 기존 엔진의
호환성을 위해 유지한다. 면제 API·화면의 보류 결정은 그대로다.

## 파일 대응

Kotlin은 `core/src/main/kotlin/app/jeongsan/core/`, Java는
`core/src/main/java/app/jeongsan/core/javaimpl/`에 있다.

| 기능 | Kotlin 파일·타입 | Java 파일·타입 |
|---|---|---|
| 입력·출석·술병 합산 | `Model.kt` / `SettlementInput` 등 | `Model.java` / `Model.SettlementInput` 등 |
| 정확한 유리수 | `Rational.kt` / `Rational` | `Rational.java` / `Rational` |
| SAVE·CONFIRM 검증 | `Validation.kt` / `Validator` | `Validation.java` / `Validation` |
| 결제자별 정산·송금 | `Settlement.kt` / `Settlement` | `Settlement.java` / `Settlement` |
| 결과·근거·성공/실패 | `SettlementResult.kt` | `SettlementResult.java` / 내부 record·sealed interface |

Java 호출 예: `app.jeongsan.core.javaimpl.Settlement.settle(Model.SettlementInput)`.
같은 JVM 이름의 충돌을 별도 패키지로 피한다. Java의 여러 공개 입력·결과 타입은 같은
기능 파일을 비교할 수 있도록 내부 record로 묶었다. Java record도 내부 컬렉션까지
불변으로 만들지는 않으므로 입력·결과의 리스트와 맵은 방어적 복사한다.

## 검증

`./gradlew :core:test :server:compileKotlin`으로 실행한다.
`JavaParitySpec.kt`는 Java 입력으로 변환해 두 엔진을 실제 호출하고 결과를 Kotlin
값 객체로 정규화하여 **모든 필드**를 비교한다. 유리수는 문자열이 아닌 정확한
분자·분모로 비교한다. 오류 메시지·식별자·순서와 맵 키·송금 순서도 확인한다.

기존 48건 + 비교 스펙 6건 = 54건 통과(실패·건너뛰기 0).
비교 스펙에는 고정 seed `20261005`의 2,000개 입력, 15개 오류 코드 전체,
음수/Long 경계의 유리수, 결제자 불참·면제, 숫자 id 동률, 차수 합산 후 올림,
양방향 송금, 병 수 곱셈·누적 오버플로, 컬렉션 변경 방지가 포함된다.
비교만으로 공통 결함을 찾을 수 없으므로 대표 명세 예제의 금액·송금 기대값도 검사한다.

## 확인이 필요한 기존 동작

`CALC_RULES_V2.md` §5.1은 전체 총액 상한을 SAVE·CONFIRM 공통 검증으로 설명하지만,
PR #7의 `Validator.validateOnConfirm`은 CONFIRM에서만 검사한다.
이번 Java 구현은 Kotlin과의 호환성을 위해 그 시점을 유지한다.
두 언어가 같다는 테스트 결과가 이 명세 차이를 해소하지는 않는다.

## PR과 학습

이 작업의 기반은 PR #7의 `7cd7ff1c8e8442733d11590df17eea119fc63a27`이다.
PR #7이 미병합 상태여서 추가 변경만 검토할 수 있도록 PR 대상 브랜치도
`feat/core-v2-v3`로 둔다. PR #7의 상태가 바뀌면 사용자가 병합 전에 대상 브랜치를 점검한다.

[스터디 가이드](study-guides/core-v2-java-v1/guide.md)는 최신 origin/main의 실제
목차에서 읽을 위치를 가져오고, 이번 Java 추가 diff와 기존 Kotlin 대응 함수를 구분한다.
Kotlin 비교 테스트에서 새로 사용한 문법도 별도 항목으로 기록한다.
리뷰 후 가이드가 달라지면 새 버전으로 보존한다. n8n·Slack 전달은 기존 흐름을 사용하며,
이 PR에서는 워크플로 설정을 바꾸지 않는다.
