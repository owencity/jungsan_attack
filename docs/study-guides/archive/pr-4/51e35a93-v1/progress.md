# PR #4 학습 기록

상태: UNRECORDED — 사용자의 학습 완료 여부를 추정하지 않는다.

- [ ] KB-14 코틀린에서 다양한 클래스를 다루는 방법 (Kotlin 기본 / 섹션 4. 코틀린에서의 OOP / 14강) → Attendance (core/src/main/kotlin/app/jeongsan/core/Model.kt:88)
- [ ] KB-18 코틀린에서 컬렉션을 함수형으로 다루는 방법 (Kotlin 기본 / 섹션 5. 코틀린에서의 FP / 18강) → Settlement.compute (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:91)
- [ ] KB-18 코틀린에서 컬렉션을 함수형으로 다루는 방법 (Kotlin 기본 / 섹션 5. 코틀린에서의 FP / 18강) → Settlement.compute (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:108)
- [ ] KB-02 코틀린에서 null을 다루는 방법 (Kotlin 기본 / 섹션 2. 코틀린에서 변수와 타입, 연산자를 다루는 방법 / 2강) → Settlement.settle (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:22)
- [ ] KB-14 코틀린에서 다양한 클래스를 다루는 방법 (Kotlin 기본 / 섹션 4. 코틀린에서의 OOP / 14강) → RecipientSettlement (core/src/main/kotlin/app/jeongsan/core/SettlementResult.kt:35)
- [ ] JDK BigInteger — 곱셈·누적과 Long 범위 변환 → SettlementInput.effectiveRounds (core/src/main/kotlin/app/jeongsan/core/Model.kt:36)
- [ ] Rational.ceilTo — 수취인별 합산·1원 올림·잔액 보존 → Settlement.compute (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:116)
- [ ] 수취인별 방향 송금 — 자기 송금·0원 송금 제외 → Settlement.buildTransfers (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:168)
- [ ] ValidationPhase.CONFIRM — AttendanceKey 누락과 ABSENT 기본값 → Validator.validateOnConfirm (core/src/main/kotlin/app/jeongsan/core/Validation.kt:223)
- [ ] SettlementOutcome.Failure — 음수 조정액의 오류 반환 → Settlement.negativeAmountError (core/src/main/kotlin/app/jeongsan/core/Settlement.kt:31)
- [ ] BigInteger.fold — 차수 상한과 전체 합계 상한 검증 → Validator.validateOnConfirm (core/src/main/kotlin/app/jeongsan/core/Validation.kt:198)

## 메모

- diff에서 확인한 점:
- 다시 볼 점:
- 사용자가 기록한 학습 날짜:
