package app.jeongsan.core

import app.jeongsan.core.javaimpl.Model as JModel
import app.jeongsan.core.javaimpl.Rational as JRational
import app.jeongsan.core.javaimpl.Settlement as JSettlement
import app.jeongsan.core.javaimpl.SettlementResult as JResult
import app.jeongsan.core.javaimpl.Validation as JValidation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import kotlin.random.Random

/** 금액만 같아도 틀릴 수 있어 근거의 유리수, 오류 위치, 송금 순서까지 비교한다. */
class JavaParitySpec : StringSpec({
    "유리수의 정규화·음수 올림·표시·비교와 계약 위반이 두 언어에서 같다" {
        for (n in -40L..40L) for (d in 1L..17L) {
            val k = Rational.of(BigInteger.valueOf(n), BigInteger.valueOf(-d))
            val j = JRational.of(BigInteger.valueOf(n), BigInteger.valueOf(-d))
            j.toKotlin() shouldBe k
            j.hashCode() shouldBe k.hashCode()
            j.signum() shouldBe k.signum
            j.isZero() shouldBe k.isZero
            j.toString() shouldBe k.toString()
            for (unit in listOf(1, 10, 100)) j.ceilTo(unit) shouldBe k.ceilTo(unit)
            for (decimals in listOf(0, 2, 6)) j.toDisplayString(decimals) shouldBe k.toDisplayString(decimals)
            j.plus(JRational.of(7)).toKotlin() shouldBe k + Rational.of(7)
            j.minus(JRational.of(7)).toKotlin() shouldBe k - Rational.of(7)
            j.divide(-3).toKotlin() shouldBe k / -3
            j.compareTo(JRational.of(1)).coerceIn(-1, 1) shouldBe k.compareTo(Rational.of(1)).coerceIn(-1, 1)
            j shouldBe JRational.of(BigInteger.valueOf(n * 3), BigInteger.valueOf(-d * 3))
        }
        shouldThrow<IllegalArgumentException> { JRational.of(BigInteger.ONE, BigInteger.ZERO) }
        shouldThrow<IllegalArgumentException> { JRational.ZERO.divide(0) }
        shouldThrow<IllegalArgumentException> { JRational.ZERO.ceilTo(0) }
        shouldThrow<ArithmeticException> { JRational.of(BigInteger.ONE.shiftLeft(100), BigInteger.ONE).ceilTo(1) }
        JRational.of(Long.MAX_VALUE).ceilTo(1) shouldBe Long.MAX_VALUE
        JRational.of(Long.MIN_VALUE).ceilTo(1) shouldBe Long.MIN_VALUE
    }

    "동일 결제자의 차수 몫을 합친 뒤 올림하고 반대 방향 송금을 상계하지 않는다" {
        val ps = listOf(Participant(10, "결제자"), Participant(2, "B"), Participant(30, "C"))
        val rounds = listOf(round(200, 2, 10, 0, 10), round(100, 1, 10, 0, 10))
        val input = completeInput(ps, rounds)
        assertParity(input)
        val result = input.succeed()
        result.amounts shouldBe mapOf(10L to 6L, 2L to 7L, 30L to 7L)
        result.breakdown.getValue(2).rounds.map { it.roundId } shouldBe listOf(100L, 200L)

        val twoPayers = completeInput(ps.take(2), listOf(round(1, 1, 100, 0, 10), round(2, 2, 100, 0, 2)))
        assertParity(twoPayers)
        twoPayers.succeed().transfers shouldBe listOf(Transfer(10, 2, 50), Transfer(2, 10, 50))
    }

    "결제자가 불참·면제이면 최대 원부담자의 숫자 id 동률 규칙을 적용하고 음수 조정은 실패한다" {
        val ps = listOf(Participant(10, "A"), Participant(2, "B"), Participant(30, "결제자"))
        for (payerAttendance in listOf(Attendance.ABSENT, Attendance.EXEMPT)) {
            val input = completeInput(ps, listOf(round(1, 1, 11, 0, 30))).copy(
                attendance = mapOf(AttendanceKey(10, 1) to Attendance.SOBER,
                    AttendanceKey(2, 1) to Attendance.SOBER, AttendanceKey(30, 1) to payerAttendance),
            )
            assertParity(input)
            input.succeed().recipients.getValue(30).adjustmentParticipantId shouldBe 2L
            input.succeed().amounts shouldBe mapOf(10L to 6L, 2L to 5L, 30L to 0L)
        }
        val tiny = completeInput(ps, listOf(round(1, 1, 1, 0, 30)))
        assertParity(tiny)
        tiny.fail().single().code shouldBe ErrorCode.NEGATIVE_ADJUSTED_AMOUNT
    }

    "검증 단계별 오류 코드·메시지·식별자·발생 순서와 술병 오버플로가 같다" {
        val ps = participants("A", "B", "C")
        val valid = completeInput(ps, listOf(round(1, 1, 100, 20, 1)))
        val cases = listOf(
            valid, SettlementInput(emptyList(), emptyList()), valid.copy(participants = ps.take(1)),
            valid.copy(participants = ps + ps.first()), valid.copy(rounds = valid.rounds + valid.rounds.first()),
            valid.copy(rounds = valid.rounds + round(2, 1, 100, 0, 1)),
            valid.copy(rounds = listOf(round(1, 1, 0, -1, 99))),
            valid.copy(rounds = listOf(round(1, 1, 10, 11, 1))),
            valid.copy(attendance = emptyMap()),
            valid.copy(attendance = valid.attendance + (AttendanceKey(99, 98) to Attendance.SOBER)),
            valid.copy(attendance = ps.associate { AttendanceKey(it.id, 1) to Attendance.EXEMPT }),
            valid.copy(attendance = ps.associate { AttendanceKey(it.id, 1) to Attendance.SOBER }),
            valid.copy(drinkItems = listOf(DrinkItem(99, "잘못된 병", 0, -1))),
            valid.copy(drinkItems = listOf(DrinkItem(1, "정상 병", 2, 10))),
            valid.copy(drinkItems = listOf(DrinkItem(1, "곱셈 넘침", Int.MAX_VALUE, Long.MAX_VALUE))),
            valid.copy(drinkItems = listOf(DrinkItem(1, "음수 넘침", Int.MAX_VALUE, Long.MIN_VALUE))),
            valid.copy(drinkItems = listOf(DrinkItem(1, "누적 넘침", 1, Long.MAX_VALUE),
                DrinkItem(1, "누적 넘침2", 1, Long.MAX_VALUE))),
            completeInput(ps, listOf(round(1, 1, Long.MAX_VALUE, 0, 1), round(2, 2, Long.MAX_VALUE, 0, 1))),
            completeInput(ps, listOf(round(1, 1, Validator.MAX_AMOUNT, 0, 1))),
            completeInput(ps, listOf(round(1, 1, Validator.MAX_AMOUNT, 0, 1), round(2, 2, 1, 0, 1))),
            completeInput(ps, listOf(round(1, 1, 1, 0, 1))),
        )
        val seen = mutableSetOf<ErrorCode>()
        for (input in cases) {
            assertParity(input)
            for (phase in ValidationPhase.entries) seen += Validator.validate(input, phase).map { it.code }
            val outcome = Settlement.settle(input)
            if (outcome is SettlementOutcome.Failure) seen += outcome.errors.map { it.code }
        }
        seen shouldBe ErrorCode.entries.toSet()
        Validator.validate(valid.copy(attendance = emptyMap()), ValidationPhase.SAVE) shouldBe emptyList()
    }

    "고정 seed 2000건의 정상·미소 금액 입력에서 전체 결과와 저장·확정 검증이 같다" {
        val random = Random(20261005)
        var successes = 0
        var negatives = 0
        repeat(2_000) { case ->
            val ps = (0 until random.nextInt(2, 9)).map { Participant((it * 10 + 2).toLong(), "P$it") }.shuffled(random)
            val rounds = (1..random.nextInt(1, 7)).map { seq ->
                val total = if (case % 7 == 0) 1L else random.nextLong(1, 1_000_000)
                round(seq.toLong(), seq, total, random.nextLong(0, total + 1), ps.random(random).id)
            }.shuffled(random)
            val attendance = rounds.flatMap { r -> ps.mapIndexed { index, p ->
                AttendanceKey(p.id, r.id) to if (index == 0) Attendance.DRANK else Attendance.entries.random(random)
            } }.toMap()
            val items = rounds.filter { it.alcohol > 0 && random.nextBoolean() }
                .map { DrinkItem(it.id, "소주", 1, it.alcohol) }
            val input = SettlementInput(ps, rounds, attendance, items)
            assertParity(input)
            when (val outcome = Settlement.settle(input)) {
                is SettlementOutcome.Success -> {
                    successes++
                    outcome.result.amounts.values.sum() shouldBe rounds.sumOf { it.total }
                    outcome.result.recipients.values.forEach { r -> r.participantAmounts.values.sum() shouldBe r.paidTotal }
                }
                is SettlementOutcome.Failure -> {
                    negatives++
                    outcome.errors.single().code shouldBe ErrorCode.NEGATIVE_ADJUSTED_AMOUNT
                }
            }
        }
        (successes > 1_000) shouldBe true
        (negatives > 0) shouldBe true
    }

    "Java 입력과 결과의 컬렉션은 외부 변경으로 다음 계산을 바꾸지 않는다" {
        val ps = mutableListOf(JModel.Participant(1, "A"), JModel.Participant(2, "B"))
        val rounds = mutableListOf(JModel.Round(1, 1, "1차", 10, 0, 1))
        val attendance = linkedMapOf(JModel.AttendanceKey(1, 1) to JModel.Attendance.SOBER,
            JModel.AttendanceKey(2, 1) to JModel.Attendance.SOBER)
        val input = JModel.SettlementInput(ps, rounds, attendance)
        ps.clear(); rounds.clear(); attendance.clear()
        val result = (JSettlement.settle(input) as JResult.Success).result()
        result.amounts() shouldBe mapOf(1L to 5L, 2L to 5L)
        shouldThrow<UnsupportedOperationException> { result.amounts()[1L] = 99L }
        shouldThrow<UnsupportedOperationException> { result.recipients().getValue(1).participantAmounts()[1L] = 99L }
        shouldThrow<UnsupportedOperationException> { result.breakdown().getValue(1).rounds().clear() }
        JSettlement.settle(input) shouldBe JResult.Success(result)
    }
})

private fun completeInput(ps: List<Participant>, rounds: List<Round>): SettlementInput = SettlementInput(
    ps, rounds, rounds.flatMap { round -> ps.map { AttendanceKey(it.id, round.id) to Attendance.DRANK } }.toMap(),
)

private fun SettlementInput.toJava() = JModel.SettlementInput(
    participants.map { JModel.Participant(it.id, it.name) },
    rounds.map { JModel.Round(it.id, it.seq, it.label, it.total, it.alcohol, it.payerId) },
    attendance.entries.associate { JModel.AttendanceKey(it.key.participantId, it.key.roundId) to JModel.Attendance.valueOf(it.value.name) },
    drinkItems.map { JModel.DrinkItem(it.roundId, it.name, it.bottleCount, it.unitPrice) },
)

private fun JRational.toKotlin() = Rational.of(numerator(), denominator())
private fun JValidation.ValidationError.toKotlin() = ValidationError(ErrorCode.valueOf(code().name), message(), roundId(), participantId())

private fun JResult.toKotlin() = SettlementResult(
    amounts(),
    breakdown().mapValues { (_, p) -> ParticipantBreakdown(p.participantId(), p.name(), p.rounds().map { r ->
        RoundBreakdown(r.roundId(), r.seq(), r.label(), r.attended(), r.drank(), r.exempt(),
            r.foodTotal(), r.attendeeCount(), r.foodShare().toKotlin(), r.alcoholTotal(), r.drinkerCount(), r.alcoholShare().toKotlin())
    }, p.rawTotal().toKotlin(), p.finalAmount(), p.paidTotal()) },
    recipients().mapValues { (_, r) -> RecipientSettlement(r.recipientId(), r.paidTotal(), r.participantAmounts(),
        r.ownShare(), r.incomingTotal(), r.adjustmentParticipantId()) },
    transfers().map { Transfer(it.fromId(), it.toId(), it.amount()) }, grandTotal(),
)

private fun assertParity(input: SettlementInput) {
    val javaInput = input.toJava()
    javaInput.effectiveRounds().map { Round(it.id(), it.seq(), it.label(), it.total(), it.alcohol(), it.payerId()) } shouldBe input.effectiveRounds()
    for (phase in ValidationPhase.entries) {
        JValidation.validate(javaInput, JValidation.ValidationPhase.valueOf(phase.name)).map { it.toKotlin() } shouldBe Validator.validate(input, phase)
    }
    val kotlinOutcome = Settlement.settle(input)
    when (val javaOutcome = JSettlement.settle(javaInput)) {
        is JResult.Failure -> SettlementOutcome.Failure(javaOutcome.errors().map { it.toKotlin() }) shouldBe kotlinOutcome
        is JResult.Success -> {
            val result = javaOutcome.result().toKotlin()
            SettlementOutcome.Success(result) shouldBe kotlinOutcome
            val expected = (kotlinOutcome as SettlementOutcome.Success).result
            result.recipients.keys.toList() shouldBe expected.recipients.keys.toList()
            result.amounts.keys.toList() shouldBe expected.amounts.keys.toList()
            result.breakdown.keys.toList() shouldBe expected.breakdown.keys.toList()
            result.recipients.forEach { (id, r) ->
                r.participantAmounts.keys.toList() shouldBe expected.recipients.getValue(id).participantAmounts.keys.toList()
            }
        }
    }
}
