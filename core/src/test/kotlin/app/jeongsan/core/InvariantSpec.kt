package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * `CALC_RULES.md` §5 불변식 테스트.
 *
 * T1~T11은 **아는 케이스**만 막는다. 여기서는 랜덤 입력 30,000건에 대해
 * 세 가지가 항상 참인지 확인한다. 시드가 고정돼 있어 실패는 항상 재현된다.
 */
class InvariantSpec : StringSpec({

    val cases = 30_000

    "합계는 언제나 원금과 정확히 일치한다" {
        var verified = 0
        repeat(cases) { seed ->
            val result = scenario(seed).settleOrNull() ?: return@repeat
            result.amounts.values.sum() shouldBe result.grandTotal
            verified++
        }
        verified shouldBeGreaterThan cases / 2
    }

    "모든 최종 금액이 0 이상이다 — 대표결제자 포함 (T7 회귀)" {
        var verified = 0
        repeat(cases) { seed ->
            val result = scenario(seed).settleOrNull() ?: return@repeat
            val negative = result.amounts.filterValues { it < 0 }
            if (negative.isNotEmpty()) {
                error("seed=$seed 에서 음수 금액: $negative (unit=${result.appliedRoundingUnit})")
            }
            verified++
        }
        verified shouldBeGreaterThan cases / 2
    }

    "같은 입력은 항상 같은 출력을 낸다 — 금액·대표결제자·송금 순서 전부" {
        repeat(2_000) { seed ->
            val input = scenario(seed)
            val a = input.settleOrNull() ?: return@repeat
            val b = input.settleOrNull() ?: return@repeat
            a.mainPayerId shouldBe b.mainPayerId
            a.amounts shouldBe b.amounts
            a.transfers shouldBe b.transfers
        }
    }

    "송금 목록의 총액은 채권자들이 받아야 할 금액과 일치한다" {
        repeat(5_000) { seed ->
            val result = scenario(seed).settleOrNull() ?: return@repeat
            val net = result.breakdown.values.associate {
                it.participantId to (it.paidTotal - it.finalAmount)
            }
            // 보내는 쪽은 줄고(−), 받는 쪽은 는다(+). 순잔액과 부호가 같아야 한다.
            val moved = mutableMapOf<Long, Long>()
            result.transfers.forEach {
                moved[it.fromId] = (moved[it.fromId] ?: 0L) - it.amount
                moved[it.toId] = (moved[it.toId] ?: 0L) + it.amount
            }
            net.forEach { (id, balance) ->
                (moved[id] ?: 0L) shouldBe balance
            }
        }
    }
})

private fun SettlementInput.settleOrNull(): SettlementResult? =
    when (val outcome = Settlement.settle(this)) {
        is SettlementOutcome.Success -> outcome.result
        is SettlementOutcome.Failure -> null    // 검증에 걸린 입력은 불변식 대상이 아니다
    }

/** 2~8명 / 1~4차수 / unit 10 또는 100. 시드가 같으면 항상 같은 시나리오다. */
private fun scenario(seed: Int): SettlementInput {
    val rnd = Random(seed)
    val n = rnd.nextInt(2, 9)
    val ps = (0 until n).map { Participant(id = (it + 1).toLong(), name = "P$it") }

    val rounds = mutableListOf<Round>()
    val att = mutableMapOf<AttendanceKey, Attendance>()

    repeat(rnd.nextInt(1, 5)) { i ->
        val roundId = (i + 1).toLong()
        val attendees = ps.filter { rnd.nextDouble() < 0.8 }.ifEmpty { listOf(ps[0]) }.toSet()
        val drinkers = attendees.filter { rnd.nextDouble() < 0.7 }.toSet()
        val total = rnd.nextInt(1, 41) * 1_000L
        val alcohol = if (drinkers.isEmpty()) 0L else rnd.nextInt(0, (total / 1_000).toInt() + 1) * 1_000L

        rounds += Round(roundId, i + 1, "${i + 1}차", total, alcohol, ps[rnd.nextInt(n)].id)
        ps.forEach { p ->
            att[AttendanceKey(p.id, roundId)] = when {
                p in drinkers -> Attendance.DRANK
                p in attendees -> Attendance.SOBER
                else -> Attendance.ABSENT
            }
        }
    }

    return SettlementInput(
        participants = ps,
        rounds = rounds,
        attendance = att,
        roundingUnit = if (rnd.nextBoolean()) 10 else 100,
    )
}
