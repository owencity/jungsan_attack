package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * `CALC_RULES.md` §3의 T1~T11을 그대로 옮긴 것이다.
 * 기대값은 참조 구현(`docs/reference_impl.py`)으로 실제 계산해 검증된 값이다.
 * **구현 결과가 다르면 구현이 틀린 것이다.**
 */
class SettlementSpec : StringSpec({

    "T1 · 기본 N빵 — 나머지가 대표결제자에게 간다" {
        val ps = participants("A", "B", "C")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 10_000, alcohol = 0, payerId = ps.id("A"))),
            attendance = ps.attendance { sober(1L, "A", "B", "C") },
        ).succeed()

        result.mainPayerName(ps) shouldBe "A"
        result.amountsByName(ps) shouldBe mapOf("A" to 3_320L, "B" to 3_340L, "C" to 3_340L)
        result.amounts.values.sum() shouldBe 10_000L
        result.transfersByName(ps) shouldBe listOf("B→A 3340", "C→A 3340")
    }

    "T2 · 2차 불참 — 올림이 없으면 대표결제자도 이득을 보지 않는다" {
        val ps = participants("A", "B", "C", "D")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("A")),
                round(2L, 2, total = 30_000, alcohol = 0, payerId = ps.id("A")),
            ),
            attendance = ps.attendance {
                sober(1L, "A", "B", "C", "D")
                sober(2L, "A", "B", "D")
                absent(2L, "C")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe
            mapOf("A" to 20_000L, "B" to 20_000L, "C" to 10_000L, "D" to 20_000L)
        result.amounts.values.sum() shouldBe 70_000L
    }

    "T3 · 논알콜 분리" {
        val ps = participants("A", "B", "C", "D")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 20_000, payerId = ps.id("A"))),
            attendance = ps.attendance {
                drank(1L, "A", "B", "C")
                sober(1L, "D")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe
            mapOf("A" to 11_660L, "B" to 11_670L, "C" to 11_670L, "D" to 5_000L)
        result.amounts.values.sum() shouldBe 40_000L
    }

    "T4 · 결제자가 두 명 — 금액은 T2와 같지만 송금 목록이 다르다" {
        val ps = participants("A", "B", "C", "D")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("A")),
                round(2L, 2, total = 30_000, alcohol = 0, payerId = ps.id("B")),
            ),
            attendance = ps.attendance {
                sober(1L, "A", "B", "C", "D")
                sober(2L, "A", "B", "D")
                absent(2L, "C")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe
            mapOf("A" to 20_000L, "B" to 20_000L, "C" to 10_000L, "D" to 20_000L)
        result.transfersByName(ps) shouldBe listOf("D→A 20000", "C→B 10000")
    }

    "T5 · 복합 — E가 두 사람에게 나눠 보내는 것이 정상이다" {
        val ps = participants("A", "B", "C", "D", "E")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 87_000, alcohol = 35_000, payerId = ps.id("A")),
                round(2L, 2, total = 42_000, alcohol = 30_000, payerId = ps.id("B")),
            ),
            attendance = ps.attendance {
                drank(1L, "A", "B", "C", "D")
                sober(1L, "E")
                drank(2L, "A", "B", "C")
                absent(2L, "D", "E")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "A" to 33_150L, "B" to 33_150L, "C" to 33_150L, "D" to 19_150L, "E" to 10_400L,
        )
        result.amounts.values.sum() shouldBe 129_000L
        result.transfersByName(ps) shouldBe
            listOf("C→A 33150", "D→A 19150", "E→A 1550", "E→B 8850")
    }

    "T6 · unit=100 — 단위가 커질수록 대표결제자의 이득이 커진다" {
        val ps = participants("A", "B", "C")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 10_000, alcohol = 0, payerId = ps.id("A"))),
            attendance = ps.attendance { sober(1L, "A", "B", "C") },
            roundingUnit = 100,
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf("A" to 3_200L, "B" to 3_400L, "C" to 3_400L)
        result.appliedRoundingUnit shouldBe 100
        result.roundingUnitDowngraded shouldBe false
    }

    "T7 · unit=1000은 거부된다 — 대표결제자를 음수로 만들기 때문이다" {
        val ps = participants("A", "B", "C", "D")
        val errors = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 4_100, alcohol = 0, payerId = ps.id("A"))),
            attendance = ps.attendance { sober(1L, "A", "B", "C", "D") },
            roundingUnit = 1_000,
        ).fail()

        errors.codes() shouldBe setOf(ErrorCode.INVALID_ROUNDING_UNIT)
    }

    "T7-b · unit=100에서 대표결제자가 음수가 되면 10원으로 강등해 재계산한다" {
        // 15명 / 총액 15,100원 → 1인당 1,006.67원.
        // unit=100이면 14명이 1,100원씩 = 15,400원이라 대표결제자가 −300원이 된다.
        val names = (0 until 15).map { "p%02d".format(it) }
        val ps = participants(*names.toTypedArray())
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 15_100, alcohol = 0, payerId = ps.id("p00"))),
            attendance = ps.attendance { sober(1L, *names.toTypedArray()) },
            roundingUnit = 100,
        ).succeed()

        result.roundingUnitDowngraded shouldBe true
        result.appliedRoundingUnit shouldBe 10
        result.amounts.values.all { it >= 0 } shouldBe true
        result.amounts.values.sum() shouldBe 15_100L
        result.amountsByName(ps)["p00"] shouldBe 960L    // 15,100 − (14 × 1,010)
    }

    "T8 · 면제자 — 면제자의 몫을 나머지가 나눠 낸다" {
        val ps = participants("동규", "민지", "재훈", "수아")
            .map { if (it.name == "수아") it.exempted() else it }
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("동규"))),
            attendance = ps.attendance { sober(1L, "동규", "민지", "재훈", "수아") },
        ).succeed()

        result.mainPayerName(ps) shouldBe "동규"
        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 13_320L, "민지" to 13_340L, "재훈" to 13_340L, "수아" to 0L,
        )
        result.amounts.values.sum() shouldBe 40_000L
        result.transfersByName(ps) shouldBe listOf("민지→동규 13340", "재훈→동규 13340")
    }

    "T9 · 기타 항목 — 항목 결제자가 순잔액에 반영된다" {
        val ps = participants("동규", "민지", "재훈", "수아")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("동규"))),
            attendance = ps.attendance { sober(1L, "동규", "민지", "재훈", "수아") },
            extras = listOf(
                ExtraItem(101L, "택시비", 18_000, payerId = ps.id("재훈"),
                    bearerIds = listOf(ps.id("재훈"), ps.id("수아"))),
            ),
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 10_000L, "민지" to 10_000L, "재훈" to 19_000L, "수아" to 19_000L,
        )
        result.amounts.values.sum() shouldBe 58_000L
        // 재훈은 19,000을 부담하지만 택시비 18,000을 이미 냈으므로 1,000만 보낸다.
        result.transfersByName(ps) shouldBe
            listOf("수아→동규 19000", "민지→동규 10000", "재훈→동규 1000")
    }

    "T10 · 술병 계산 — DrinkItem이 alcoholAmount를 덮어쓴다" {
        val ps = participants("동규", "민지", "재훈", "수아")
        val result = SettlementInput(
            participants = ps,
            // alcohol 을 일부러 0으로 두어 DrinkItem 이 덮어쓰는지 확인한다.
            rounds = listOf(round(1L, 1, total = 91_000, alcohol = 0, payerId = ps.id("동규"))),
            attendance = ps.attendance {
                drank(1L, "동규", "민지", "재훈")
                sober(1L, "수아")
            },
            drinkItems = listOf(
                DrinkItem(1L, "소주", bottleCount = 5, unitPrice = 5_000),
                DrinkItem(1L, "맥주", bottleCount = 3, unitPrice = 6_000),
            ),
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 26_320L, "민지" to 26_340L, "재훈" to 26_340L, "수아" to 12_000L,
        )
        result.amounts.values.sum() shouldBe 91_000L
    }

    "T11 · 전체 통합 — 면제 + 기타 항목 + 다중 결제자" {
        val ps = participants("동규", "민지", "재훈", "수아", "지원")
            .map { if (it.name == "지원") it.exempted() else it }
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 87_000, alcohol = 35_000, payerId = ps.id("동규")),
                round(2L, 2, total = 42_000, alcohol = 30_000, payerId = ps.id("민지")),
            ),
            attendance = ps.attendance {
                drank(1L, "동규", "민지", "재훈", "수아")
                sober(1L, "지원")
                drank(2L, "동규", "민지", "재훈")
                absent(2L, "수아", "지원")
            },
            extras = listOf(
                ExtraItem(101L, "택시비", 20_000, payerId = ps.id("민지"),
                    bearerIds = listOf(ps.id("수아"), ps.id("지원"))),
            ),
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 35_750L, "민지" to 35_750L, "재훈" to 35_750L,
            "수아" to 41_750L, "지원" to 0L,
        )
        result.amounts.values.sum() shouldBe 149_000L
        result.transfersByName(ps) shouldBe
            listOf("수아→동규 41750", "재훈→동규 9500", "재훈→민지 26250")
    }

    "면제자가 결제하면 전액 돌려받는다 — CALC_RULES §2.1-c" {
        // 명세에 있는 규칙인데 T8·T11 어디에도 면제자가 결제자인 케이스가 없었다.
        // 코드 리뷰 F3에서 찾은 공백을 메우는 테스트다.
        val ps = participants("동규", "민지", "재훈", "수아")
            .map { if (it.name == "수아") it.exempted() else it }
        val result = SettlementInput(
            participants = ps,
            // 면제자인 수아가 결제한다
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("수아"))),
            attendance = ps.attendance { sober(1L, "동규", "민지", "재훈", "수아") },
        ).succeed()

        result.amountsByName(ps)["수아"] shouldBe 0L                        // 부담 0
        result.breakdown.getValue(ps.id("수아")).paidTotal shouldBe 40_000L  // 결제 40,000
        result.amounts.values.sum() shouldBe 40_000L
        // 수아는 부담 0에 결제 40,000이므로 전액 회수한다.
        // 채무자는 금액 내림차순이라 13,340인 민지·재훈이 먼저, 대표결제자 동규(13,320)가 마지막이다.
        result.transfersByName(ps) shouldBe
            listOf("민지→수아 13340", "재훈→수아 13340", "동규→수아 13320")
        // 대표결제자는 면제자를 제외한 사람 중에서 뽑힌다. 수아가 40,000을 결제했지만
        // 면제자이므로 후보가 아니고, 나머지는 결제 0으로 동률이라 id가 가장 작은 동규가 된다.
        result.mainPayerName(ps) shouldBe "동규"
    }

    "breakdown은 불참 차수도 0원으로 채워진다 — W2가 '2차 불참 → 0원'을 보여줘야 한다" {
        val ps = participants("A", "B", "C", "D", "E")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 87_000, alcohol = 35_000, payerId = ps.id("A")),
                round(2L, 2, total = 42_000, alcohol = 30_000, payerId = ps.id("B")),
            ),
            attendance = ps.attendance {
                drank(1L, "A", "B", "C", "D")
                sober(1L, "E")
                drank(2L, "A", "B", "C")
                absent(2L, "D", "E")
            },
        ).succeed()

        val e = result.breakdown.getValue(ps.id("E"))
        e.rounds.size shouldBe 2
        e.rounds[0].attended shouldBe true
        e.rounds[0].drank shouldBe false
        e.rounds[0].foodTotal shouldBe 52_000L      // 87,000 − 35,000
        e.rounds[0].attendeeCount shouldBe 5        // 안주값 52,000 ÷ 5명
        e.rounds[0].amount shouldBe Rational.of(10_400)
        e.rounds[1].attended shouldBe false
        e.rounds[1].amount shouldBe Rational.ZERO
        e.finalAmount shouldBe 10_400L
    }
})
