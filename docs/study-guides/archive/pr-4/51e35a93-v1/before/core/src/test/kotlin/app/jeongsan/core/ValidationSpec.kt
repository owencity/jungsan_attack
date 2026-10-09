package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** `CALC_RULES.md` §4 — 검증 시점이 둘로 나뉜다. */
class ValidationSpec : StringSpec({

    val base = participants("A", "B", "C", "D")

    fun input(
        ps: List<Participant> = base,
        rounds: List<Round> = listOf(round(1L, 1, 40_000, 0, ps.id("A"))),
        att: Map<AttendanceKey, Attendance> = ps.attendance { sober(1L, "A", "B", "C", "D") },
        extras: List<ExtraItem> = emptyList(),
        drinkItems: List<DrinkItem> = emptyList(),
        unit: Int = 10,
    ) = SettlementInput(ps, rounds, att, drinkItems, extras, unit)

    // ── 저장 시점에도 확정 시점에도 걸리는 형식 검증

    "총액이 0 이하면 거부한다" {
        input(rounds = listOf(round(1L, 1, 0, 0, base.id("A")))).fail()
            .codes() shouldContain ErrorCode.TOTAL_NOT_POSITIVE
    }

    "술값이 음수면 거부한다" {
        input(rounds = listOf(round(1L, 1, 40_000, -1, base.id("A")))).fail()
            .codes() shouldContain ErrorCode.ALCOHOL_NEGATIVE
    }

    "술값이 총액보다 크면 거부한다 — 어느 차수인지 알려준다" {
        val errors = input(rounds = listOf(round(1L, 1, 40_000, 50_000, base.id("A")))).fail()
        errors.codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
        errors.first { it.code == ErrorCode.ALCOHOL_EXCEEDS_TOTAL }.roundId shouldBe 1L
    }

    "총액 상한을 넘으면 거부한다 — 오타 방어 (코드 리뷰 F2)" {
        val errors = input(rounds = listOf(round(1L, 1, 9_999_999_999_999, 0, base.id("A")))).fail()
        errors.codes() shouldContain ErrorCode.AMOUNT_TOO_LARGE
    }

    "결제자가 참여자 목록에 없으면 거부한다" {
        input(rounds = listOf(round(1L, 1, 40_000, 0, payerId = 999L))).fail()
            .codes() shouldContain ErrorCode.PAYER_NOT_FOUND
    }

    "기타 항목의 부담자가 참여자 목록에 없으면 거부한다" {
        input(extras = listOf(ExtraItem(101L, "택시비", 10_000, base.id("A"), listOf(base.id("A"), 999L))))
            .fail().codes() shouldContain ErrorCode.BEARER_NOT_FOUND
    }

    "술병의 병 수나 단가가 0 이하면 거부한다" {
        input(drinkItems = listOf(DrinkItem(1L, "소주", bottleCount = 0, unitPrice = 5_000)))
            .fail().codes() shouldContain ErrorCode.INVALID_DRINK_ITEM
    }

    "반올림 단위가 10·100이 아니면 거부한다" {
        input(unit = 1_000).fail().codes() shouldContain ErrorCode.INVALID_ROUNDING_UNIT
        input(unit = 1).fail().codes() shouldContain ErrorCode.INVALID_ROUNDING_UNIT
    }

    "불참인데 음주면 거부한다 — 예외가 아니라 검증 오류다" {
        val errors = input(
            att = base.attendance { sober(1L, "A", "B", "C") } +
                mapOf(AttendanceKey(base.id("D"), 1L) to Attendance(attended = false, drank = true)),
        ).fail()
        errors.codes() shouldContain ErrorCode.DRANK_WITHOUT_ATTEND
        errors.first { it.code == ErrorCode.DRANK_WITHOUT_ATTEND }.participantId shouldBe base.id("D")
    }

    // ── 중복 id (코드 리뷰 F1)

    "참여자 id 중복을 거부하고 어느 id인지 알려준다" {
        val dup = base + Participant(base.id("A"), "가짜A")
        val errors = SettlementInput(
            dup,
            listOf(round(1L, 1, 40_000, 0, base.id("A"))),
            base.attendance { sober(1L, "A", "B", "C", "D") },
        ).fail()
        errors.codes() shouldContain ErrorCode.DUPLICATE_ID
        errors.first { it.code == ErrorCode.DUPLICATE_ID }.participantId shouldBe base.id("A")
    }

    "차수 id 중복을 거부한다 — 합계가 맞아도 잡아야 한다" {
        // 같은 roundId 가 둘이면 attendance 조회가 두 차수에 같은 값을 돌려주어
        // 1차 참석자가 2차에도 자동 참석한 것이 된다. 그런데 합계는 정확히 맞으므로
        // 불변식 테스트로도 잡히지 않는다. 검증에서 막아야 한다.
        val errors = input(
            rounds = listOf(
                round(1L, 1, 40_000, 0, base.id("A")),
                round(1L, 2, 30_000, 0, base.id("A")),
            ),
        ).fail()
        errors.codes() shouldContain ErrorCode.DUPLICATE_ID
        errors.first { it.code == ErrorCode.DUPLICATE_ID }.roundId shouldBe 1L
    }

    "기타 항목 id 중복을 거부한다" {
        val errors = input(
            extras = listOf(
                ExtraItem(101L, "택시비", 10_000, base.id("A"), listOf(base.id("A"))),
                ExtraItem(101L, "대리비", 20_000, base.id("A"), listOf(base.id("A"))),
            ),
        ).fail()
        errors.codes() shouldContain ErrorCode.DUPLICATE_ID
        errors.first { it.code == ErrorCode.DUPLICATE_ID }.extraId shouldBe 101L
    }

    "차수 순번(seq) 중복을 거부한다 — 근거 화면의 차수 순서가 흔들린다" {
        val errors = input(
            rounds = listOf(
                round(1L, 1, 40_000, 0, base.id("A")),
                round(2L, 1, 30_000, 0, base.id("A")),
            ),
        ).fail()
        errors.codes() shouldContain ErrorCode.DUPLICATE_ROUND_SEQ
    }

    // ── 확정 시점에만 걸리는 인원·배분 검증

    "참여자가 2명 미만이면 거부한다" {
        val one = participants("A")
        input(ps = one, rounds = listOf(round(1L, 1, 40_000, 0, one.id("A"))),
            att = one.attendance { sober(1L, "A") })
            .fail().codes() shouldContain ErrorCode.TOO_FEW_PARTICIPANTS
    }

    "차수 참석자가 0명이면 거부한다" {
        input(att = base.attendance { absent(1L, "A", "B", "C", "D") }).fail()
            .codes() shouldContain ErrorCode.NO_ATTENDEE
    }

    "참석자가 전원 면제자면 거부한다" {
        val ps = participants("A", "B", "C").map { if (it.name == "C") it else it.exempted() }
        input(
            ps = ps,
            rounds = listOf(round(1L, 1, 40_000, 0, ps.id("A"))),
            att = ps.attendance { sober(1L, "A", "B"); absent(1L, "C") },
        ).fail().codes() shouldContain ErrorCode.NO_NON_EXEMPT_ATTENDEE
    }

    "술값이 있는데 음주자가 0명이면 거부한다 — 안내 문구를 담는다" {
        val errors = input(rounds = listOf(round(1L, 1, 40_000, 30_000, base.id("A")))).fail()
        errors.codes() shouldContain ErrorCode.NO_DRINKER_WITH_ALCOHOL
        val e = errors.first { it.code == ErrorCode.NO_DRINKER_WITH_ALCOHOL }
        e.roundId shouldBe 1L
        e.message.contains("술값을 0으로 하거나") shouldBe true
    }

    "기타 항목의 부담자가 전원 면제자면 거부한다" {
        val ps = participants("A", "B", "C", "D").map { if (it.name == "D") it.exempted() else it }
        input(
            ps = ps,
            rounds = listOf(round(1L, 1, 40_000, 0, ps.id("A"))),
            att = ps.attendance { sober(1L, "A", "B", "C", "D") },
            extras = listOf(ExtraItem(101L, "택시비", 10_000, ps.id("A"), listOf(ps.id("D")))),
        ).fail().codes() shouldContain ErrorCode.NO_NON_EXEMPT_BEARER
    }

    "참여자 전원이 면제면 거부한다" {
        val ps = participants("A", "B").map { it.exempted() }
        input(
            ps = ps,
            rounds = listOf(round(1L, 1, 40_000, 0, ps.id("A"))),
            att = ps.attendance { sober(1L, "A", "B") },
        ).fail().codes() shouldContain ErrorCode.ALL_EXEMPT
    }

    // ── 시점 구분

    "저장 시점에는 참석자가 0명이어도 통과한다 — 아직 아무도 체크하지 않았기 때문이다" {
        val i = input(att = emptyMap())

        Validator.validate(i, ValidationPhase.SAVE) shouldBe emptyList()
        Validator.validate(i, ValidationPhase.CONFIRM)
            .codes() shouldContain ErrorCode.NO_ATTENDEE
    }

    "저장 시점에도 형식 오류는 잡는다" {
        Validator.validate(
            input(rounds = listOf(round(1L, 1, 40_000, 50_000, base.id("A"))), att = emptyMap()),
            ValidationPhase.SAVE,
        ).codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
    }

    "술값 검증은 DrinkItem으로 덮어쓴 뒤의 값으로 한다" {
        // alcohol=0 이지만 술병 합계가 50,000이라 총액 40,000을 넘는다.
        Validator.validate(
            input(
                rounds = listOf(round(1L, 1, 40_000, 0, base.id("A"))),
                drinkItems = listOf(DrinkItem(1L, "소주", 10, 5_000)),
                att = emptyMap(),
            ),
            ValidationPhase.SAVE,
        ).codes() shouldContain ErrorCode.ALCOHOL_EXCEEDS_TOTAL
    }
})
