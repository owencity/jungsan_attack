package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** `CALC_RULES.md` §4 — 검증 시점이 둘로 나뉜다. */
class ValidationSpec : StringSpec({

    fun baseInput(
        rounds: List<Round> = listOf(round("r1", 1, 40_000, 0, "A")),
        ps: List<Participant> = participants("A", "B", "C", "D"),
        att: Map<AttendanceKey, Attendance> = attendance { sober("r1", "A", "B", "C", "D") },
        extras: List<ExtraItem> = emptyList(),
        drinkItems: List<DrinkItem> = emptyList(),
        unit: Int = 10,
    ) = SettlementInput(ps, rounds, att, drinkItems, extras, unit)

    // ── 저장 시점에도 확정 시점에도 걸리는 형식 검증

    "총액이 0 이하면 거부한다" {
        baseInput(rounds = listOf(round("r1", 1, 0, 0, "A"))).fail()
            .codes() shouldContain ErrorCode.TOTAL_NOT_POSITIVE
    }

    "술값이 음수면 거부한다" {
        baseInput(rounds = listOf(round("r1", 1, 40_000, -1, "A"))).fail()
            .codes() shouldContain ErrorCode.ALCOHOL_NEGATIVE
    }

    "술값이 총액보다 크면 거부한다 — 어느 차수인지 알려준다" {
        val errors = baseInput(rounds = listOf(round("r1", 1, 40_000, 50_000, "A"))).fail()
        errors.codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
        errors.first { it.code == ErrorCode.ALCOHOL_EXCEEDS_TOTAL }.roundId shouldBe "r1"
    }

    "결제자가 참여자 목록에 없으면 거부한다" {
        baseInput(rounds = listOf(round("r1", 1, 40_000, 0, "없는사람"))).fail()
            .codes() shouldContain ErrorCode.PAYER_NOT_FOUND
    }

    "기타 항목의 부담자가 참여자 목록에 없으면 거부한다" {
        baseInput(
            extras = listOf(ExtraItem("e1", "택시비", 10_000, "A", listOf("A", "유령"))),
        ).fail().codes() shouldContain ErrorCode.BEARER_NOT_FOUND
    }

    "술병의 병 수나 단가가 0 이하면 거부한다" {
        baseInput(
            drinkItems = listOf(DrinkItem("r1", "소주", bottleCount = 0, unitPrice = 5_000)),
        ).fail().codes() shouldContain ErrorCode.INVALID_DRINK_ITEM
    }

    "반올림 단위가 10·100이 아니면 거부한다" {
        baseInput(unit = 1_000).fail().codes() shouldContain ErrorCode.INVALID_ROUNDING_UNIT
        baseInput(unit = 1).fail().codes() shouldContain ErrorCode.INVALID_ROUNDING_UNIT
    }

    "불참인데 음주면 거부한다 — 예외가 아니라 검증 오류다" {
        val errors = baseInput(
            att = attendance { sober("r1", "A", "B", "C") } +
                mapOf(AttendanceKey("D", "r1") to Attendance(attended = false, drank = true)),
        ).fail()
        errors.codes() shouldContain ErrorCode.DRANK_WITHOUT_ATTEND
        errors.first { it.code == ErrorCode.DRANK_WITHOUT_ATTEND }.participantId shouldBe "D"
    }

    // ── 확정 시점에만 걸리는 인원·배분 검증

    "참여자가 2명 미만이면 거부한다" {
        baseInput(
            ps = participants("A"),
            att = attendance { sober("r1", "A") },
        ).fail().codes() shouldContain ErrorCode.TOO_FEW_PARTICIPANTS
    }

    "차수 참석자가 0명이면 거부한다" {
        baseInput(att = attendance { absent("r1", "A", "B", "C", "D") }).fail()
            .codes() shouldContain ErrorCode.NO_ATTENDEE
    }

    "참석자가 전원 면제자면 거부한다" {
        baseInput(
            ps = participants("A", "B").map { it.exempted() } + participants("C"),
            att = attendance { sober("r1", "A", "B"); absent("r1", "C") },
        ).fail().codes() shouldContain ErrorCode.NO_NON_EXEMPT_ATTENDEE
    }

    "술값이 있는데 음주자가 0명이면 거부한다 — 안내 문구를 담는다" {
        val errors = baseInput(
            rounds = listOf(round("r1", 1, 40_000, 30_000, "A")),
        ).fail()
        errors.codes() shouldContain ErrorCode.NO_DRINKER_WITH_ALCOHOL
        val e = errors.first { it.code == ErrorCode.NO_DRINKER_WITH_ALCOHOL }
        e.roundId shouldBe "r1"
        (e.message.contains("술값을 0으로 하거나")) shouldBe true
    }

    "기타 항목의 부담자가 전원 면제자면 거부한다" {
        baseInput(
            ps = participants("A", "B", "C") + participants("D").map { it.exempted() },
            extras = listOf(ExtraItem("e1", "택시비", 10_000, "A", listOf("D"))),
        ).fail().codes() shouldContain ErrorCode.NO_NON_EXEMPT_BEARER
    }

    "참여자 전원이 면제면 거부한다" {
        baseInput(
            ps = participants("A", "B").map { it.exempted() },
            att = attendance { sober("r1", "A", "B") },
        ).fail().codes() shouldContain ErrorCode.ALL_EXEMPT
    }

    // ── 시점 구분

    "저장 시점에는 참석자가 0명이어도 통과한다 — 아직 아무도 체크하지 않았기 때문이다" {
        val input = baseInput(att = emptyMap())

        Validator.validate(input, ValidationPhase.SAVE) shouldBe emptyList()
        Validator.validate(input, ValidationPhase.CONFIRM)
            .codes() shouldContain ErrorCode.NO_ATTENDEE
    }

    "저장 시점에도 형식 오류는 잡는다" {
        Validator.validate(
            baseInput(rounds = listOf(round("r1", 1, 40_000, 50_000, "A")), att = emptyMap()),
            ValidationPhase.SAVE,
        ).codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
    }

    "술값 검증은 DrinkItem으로 덮어쓴 뒤의 값으로 한다" {
        // alcohol=0 이지만 술병 합계가 50,000이라 총액 40,000을 넘는다.
        Validator.validate(
            baseInput(
                rounds = listOf(round("r1", 1, 40_000, 0, "A")),
                drinkItems = listOf(DrinkItem("r1", "소주", 10, 5_000)),
            ),
            ValidationPhase.SAVE,
        ).codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
    }
})
