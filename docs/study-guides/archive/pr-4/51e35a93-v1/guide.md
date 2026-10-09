# PR #4 스터디 가이드 v1

- 코드 기준: `18998adee79ec358798564e470ccfab16a698643` → `51e35a93a0f9f2de02ed0b0b5949823c3bbfea72`.
- 읽을 목차는 생성 시 최신 origin/main `f094e884580159ffb82844bd3d759cf028c96458`의 카탈로그에서 그대로 가져왔다.
- 추가·수정된 줄만 학습 근거로 검증했다. 먼저 changes.patch에서 해당 줄을 체크한 뒤 목차를 읽는다.
- 이 PR의 신규 구현은 Kotlin이다. Java 기본기·Effective Java·함수형 Java 항목은 없으며, Java 대응 구현은 PR #16에서 별도로 본다.

## Java 기본기

이번 diff에서 해당 카테고리의 추천 항목 없음.

## Effective Java

이번 diff에서 해당 카테고리의 추천 항목 없음.

## 함수형 Java

이번 diff에서 해당 카테고리의 추천 항목 없음.

## Kotlin 기본

- **읽을 위치:** KB-14 코틀린에서 다양한 클래스를 다루는 방법 (Kotlin 기본 / 섹션 4. 코틀린에서의 OOP / 14강) · **코드:** [core/src/main/kotlin/app/jeongsan/core/Model.kt:88](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Model.kt#L88) → `Attendance` · **볼 관점:** 상태 상수와 attended·exempt·drank 필드를 보고 허용되지 않는 불리언 조합을 enum이 어떻게 막는지 확인한다.
- **읽을 위치:** KB-18 코틀린에서 컬렉션을 함수형으로 다루는 방법 (Kotlin 기본 / 섹션 5. 코틀린에서의 FP / 18강) · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:91](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L91) → `Settlement.compute` · **볼 관점:** payerId별 그룹을 만든 뒤 각 그룹에 속한 차수만 합산하는 변경 줄을 확인한다.
- **읽을 위치:** KB-18 코틀린에서 컬렉션을 함수형으로 다루는 방법 (Kotlin 기본 / 섹션 5. 코틀린에서의 FP / 18강) · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:108](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L108) → `Settlement.compute` · **볼 관점:** 부담액 내림차순 다음 숫자 id 오름차순이 잔액 조정자를 결정하는지 확인한다.
- **읽을 위치:** KB-02 코틀린에서 null을 다루는 방법 (Kotlin 기본 / 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법 / 2강) · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:22](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L22) → `Settlement.settle` · **볼 관점:** nullable 탐색 결과를 검사한 뒤 실패 결과를 만드는 분기와 null일 때 성공하는 경로를 확인한다.
- **읽을 위치:** KB-14 코틀린에서 다양한 클래스를 다루는 방법 (Kotlin 기본 / 섹션 4. 코틀린에서의 OOP / 14강) · **코드:** [core/src/main/kotlin/app/jeongsan/core/SettlementResult.kt:35](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/SettlementResult.kt#L35) → `RecipientSettlement` · **볼 관점:** 새 data class의 수취인·본인 부담·받을 합계 필드가 계산 결과의 어느 단위를 표현하는지 확인한다.

## Kotlin 고급

이번 diff에서 해당 카테고리의 추천 항목 없음.

## 기술별 학습 포인트

- **읽을 위치:** JDK BigInteger — 곱셈·누적과 Long 범위 변환 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Model.kt:36](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Model.kt#L36) → `SettlementInput.effectiveRounds` · **볼 관점:** 금액 오버플로: 병 수와 단가를 곱하기 전에 BigInteger로 바꾸는 위치와 Long 경계 처리의 순서를 확인한다.
- **읽을 위치:** Rational.ceilTo — 수취인별 합산·1원 올림·잔액 보존 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:116](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L116) → `Settlement.compute` · **볼 관점:** 원 단위 올림: 수취인별 정확한 합계에 한 번만 올림을 적용하고 잔액은 조정자에게 배정하는지 확인한다.
- **읽을 위치:** 수취인별 방향 송금 — 자기 송금·0원 송금 제외 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:168](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L168) → `Settlement.buildTransfers` · **볼 관점:** 송금 분리: 서로 다른 결제자 사이에 상계하지 않고 배정된 수취인별 금액을 송금으로 만드는지 확인한다.
- **읽을 위치:** ValidationPhase.CONFIRM — AttendanceKey 누락과 ABSENT 기본값 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Validation.kt:223](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Validation.kt#L223) → `Validator.validateOnConfirm` · **볼 관점:** 저장·확정 검증: 참석 기본값과 명시적인 응답 키 누락 오류를 구분하고 CONFIRM에서 거절하는 조건을 확인한다.
- **읽을 위치:** SettlementOutcome.Failure — 음수 조정액의 오류 반환 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Settlement.kt:31](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Settlement.kt#L31) → `Settlement.negativeAmountError` · **볼 관점:** 초소액 경계: 올림 뒤 조정자 부담이 음수가 되면 성공 결과를 반환하지 않고 검증 실패로 바꾸는지 확인한다.
- **읽을 위치:** BigInteger.fold — 차수 상한과 전체 합계 상한 검증 · **코드:** [core/src/main/kotlin/app/jeongsan/core/Validation.kt:198](https://github.com/owencity/jungsan_attack/blob/51e35a93a0f9f2de02ed0b0b5949823c3bbfea72/core/src/main/kotlin/app/jeongsan/core/Validation.kt#L198) → `Validator.validateOnConfirm` · **볼 관점:** 총액 상한: 차수별 상한과 전체 합계 상한이 어느 검증 단계에서 적용되는지 diff와 비교한다.
