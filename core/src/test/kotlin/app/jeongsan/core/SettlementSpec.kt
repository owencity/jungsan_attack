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
            rounds = listOf(round("r1", 1, total = 10_000, alcohol = 0, payerId = "A")),
            attendance = attendance { sober("r1", "A", "B", "C") },
        ).succeed()

        result.mainPayerId shouldBe "A"
        result.amounts shouldBe mapOf("A" to 3_320L, "B" to 3_340L, "C" to 3_340L)
        result.amounts.values.sum() shouldBe 10_000L
        result.transfers shouldBe listOf(
            Transfer("B", "A", 3_340),
            Transfer("C", "A", 3_340),
        )
    }

    "T2 · 2차 불참 — 올림이 없으면 대표결제자도 이득을 보지 않는다" {
        val result = SettlementInput(
            participants = participants("A", "B", "C", "D"),
            rounds = listOf(
                round("r1", 1, total = 40_000, alcohol = 0, payerId = "A"),
                round("r2", 2, total = 30_000, alcohol = 0, payerId = "A"),
            ),
            attendance = attendance {
                sober("r1", "A", "B", "C", "D")
                sober("r2", "A", "B", "D")
                absent("r2", "C")
            },
        ).succeed()

        result.amounts shouldBe mapOf("A" to 20_000L, "B" to 20_000L, "C" to 10_000L, "D" to 20_000L)
        result.amounts.values.sum() shouldBe 70_000L
    }

    "T3 · 논알콜 분리" {
        val result = SettlementInput(
            participants = participants("A", "B", "C", "D"),
            rounds = listOf(round("r1", 1, total = 40_000, alcohol = 20_000, payerId = "A")),
            attendance = attendance {
                drank("r1", "A", "B", "C")
                sober("r1", "D")
            },
        ).succeed()

        result.amounts shouldBe mapOf("A" to 11_660L, "B" to 11_670L, "C" to 11_670L, "D" to 5_000L)
        result.amounts.values.sum() shouldBe 40_000L
    }

    "T4 · 결제자가 두 명 — 금액은 T2와 같지만 송금 목록이 다르다" {
        val result = SettlementInput(
            participants = participants("A", "B", "C", "D"),
            rounds = listOf(
                round("r1", 1, total = 40_000, alcohol = 0, payerId = "A"),
                round("r2", 2, total = 30_000, alcohol = 0, payerId = "B"),
            ),
            attendance = attendance {
                sober("r1", "A", "B", "C", "D")
                sober("r2", "A", "B", "D")
                absent("r2", "C")
            },
        ).succeed()

        result.amounts shouldBe mapOf("A" to 20_000L, "B" to 20_000L, "C" to 10_000L, "D" to 20_000L)
        result.transfers shouldBe listOf(
            Transfer("D", "A", 20_000),
            Transfer("C", "B", 10_000),
        )
    }

    "T5 · 복합 — E가 두 사람에게 나눠 보내는 것이 정상이다" {
        val result = SettlementInput(
            participants = participants("A", "B", "C", "D", "E"),
            rounds = listOf(
                round("r1", 1, total = 87_000, alcohol = 35_000, payerId = "A"),
                round("r2", 2, total = 42_000, alcohol = 30_000, payerId = "B"),
            ),
            attendance = attendance {
                drank("r1", "A", "B", "C", "D")
                sober("r1", "E")
                drank("r2", "A", "B", "C")
                absent("r2", "D", "E")
            },
        ).succeed()

        result.amounts shouldBe mapOf(
            "A" to 33_150L, "B" to 33_150L, "C" to 33_150L, "D" to 19_150L, "E" to 10_400L,
        )
        result.amounts.values.sum() shouldBe 129_000L
        result.transfers shouldBe listOf(
            Transfer("C", "A", 33_150),
            Transfer("D", "A", 19_150),
            Transfer("E", "A", 1_550),
            Transfer("E", "B", 8_850),
        )
    }

    "T6 · unit=100 — 단위가 커질수록 대표결제자의 이득이 커진다" {
        val result = SettlementInput(
            participants = participants("A", "B", "C"),
            rounds = listOf(round("r1", 1, total = 10_000, alcohol = 0, payerId = "A")),
            attendance = attendance { sober("r1", "A", "B", "C") },
            roundingUnit = 100,
        ).succeed()

        result.amounts shouldBe mapOf("A" to 3_200L, "B" to 3_400L, "C" to 3_400L)
        result.appliedRoundingUnit shouldBe 100
        result.roundingUnitDowngraded shouldBe false
    }

    "T7 · unit=1000은 거부된다 — 대표결제자를 음수로 만들기 때문이다" {
        val errors = SettlementInput(
            participants = participants("A", "B", "C", "D"),
            rounds = listOf(round("r1", 1, total = 4_100, alcohol = 0, payerId = "A")),
            attendance = attendance { sober("r1", "A", "B", "C", "D") },
            roundingUnit = 1_000,
        ).fail()

        errors.codes() shouldBe setOf(ErrorCode.INVALID_ROUNDING_UNIT)
    }

    "T7-b · unit=100에서 대표결제자가 음수가 되면 10원으로 강등해 재계산한다" {
        // 15명 / 총액 15,100원 → 1인당 1,006.67원.
        // unit=100이면 14명이 1,100원씩 = 15,400원이라 대표결제자가 −300원이 된다.
        val ps = participants(*(0 until 15).map { "p%02d".format(it) }.toTypedArray())
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round("r1", 1, total = 15_100, alcohol = 0, payerId = "p00")),
            attendance = attendance { sober("r1", *ps.map { it.id }.toTypedArray()) },
            roundingUnit = 100,
        ).succeed()

        result.roundingUnitDowngraded shouldBe true
        result.appliedRoundingUnit shouldBe 10
        result.amounts.values.all { it >= 0 } shouldBe true
        result.amounts.values.sum() shouldBe 15_100L
        result.amounts.getValue("p00") shouldBe 960L    // 15,100 − (14 × 1,010)
    }

    "T8 · 면제자 — 면제자의 몫을 나머지가 나눠 낸다" {
        val ps = participants("동규", "민지", "재훈", "수아").map {
            if (it.id == "수아") it.exempted() else it
        }
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round("r1", 1, total = 40_000, alcohol = 0, payerId = "동규")),
            attendance = attendance { sober("r1", "동규", "민지", "재훈", "수아") },
        ).succeed()

        result.mainPayerId shouldBe "동규"
        result.amounts shouldBe mapOf(
            "동규" to 13_320L, "민지" to 13_340L, "재훈" to 13_340L, "수아" to 0L,
        )
        result.amounts.values.sum() shouldBe 40_000L
        result.transfers shouldBe listOf(
            Transfer("민지", "동규", 13_340),
            Transfer("재훈", "동규", 13_340),
        )
    }

    "T9 · 기타 항목 — 항목 결제자가 순잔액에 반영된다" {
        val result = SettlementInput(
            participants = participants("동규", "민지", "재훈", "수아"),
            rounds = listOf(round("r1", 1, total = 40_000, alcohol = 0, payerId = "동규")),
            attendance = attendance { sober("r1", "동규", "민지", "재훈", "수아") },
            extras = listOf(
                ExtraItem("e1", "택시비", 18_000, payerId = "재훈", bearerIds = listOf("재훈", "수아")),
            ),
        ).succeed()

        result.amounts shouldBe mapOf(
            "동규" to 10_000L, "민지" to 10_000L, "재훈" to 19_000L, "수아" to 19_000L,
        )
        result.amounts.values.sum() shouldBe 58_000L
        // 재훈은 19,000을 부담하지만 택시비 18,000을 이미 냈으므로 1,000만 보낸다.
        result.transfers shouldBe listOf(
            Transfer("수아", "동규", 19_000),
            Transfer("민지", "동규", 10_000),
            Transfer("재훈", "동규", 1_000),
        )
    }

    "T10 · 술병 계산 — DrinkItem이 alcoholAmount를 덮어쓴다" {
        val result = SettlementInput(
            participants = participants("동규", "민지", "재훈", "수아"),
            // alcohol 을 일부러 0으로 두어 DrinkItem 이 덮어쓰는지 확인한다.
            rounds = listOf(round("r1", 1, total = 91_000, alcohol = 0, payerId = "동규")),
            attendance = attendance {
                drank("r1", "동규", "민지", "재훈")
                sober("r1", "수아")
            },
            drinkItems = listOf(
                DrinkItem("r1", "소주", bottleCount = 5, unitPrice = 5_000),
                DrinkItem("r1", "맥주", bottleCount = 3, unitPrice = 6_000),
            ),
        ).succeed()

        result.amounts shouldBe mapOf(
            "동규" to 26_320L, "민지" to 26_340L, "재훈" to 26_340L, "수아" to 12_000L,
        )
        result.amounts.values.sum() shouldBe 91_000L
    }

    "T11 · 전체 통합 — 면제 + 기타 항목 + 다중 결제자" {
        val ps = participants("동규", "민지", "재훈", "수아", "지원").map {
            if (it.id == "지원") it.exempted() else it
        }
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round("r1", 1, total = 87_000, alcohol = 35_000, payerId = "동규"),
                round("r2", 2, total = 42_000, alcohol = 30_000, payerId = "민지"),
            ),
            attendance = attendance {
                drank("r1", "동규", "민지", "재훈", "수아")
                sober("r1", "지원")
                drank("r2", "동규", "민지", "재훈")
                absent("r2", "수아", "지원")
            },
            extras = listOf(
                ExtraItem("e1", "택시비", 20_000, payerId = "민지", bearerIds = listOf("수아", "지원")),
            ),
        ).succeed()

        result.amounts shouldBe mapOf(
            "동규" to 35_750L, "민지" to 35_750L, "재훈" to 35_750L, "수아" to 41_750L, "지원" to 0L,
        )
        result.amounts.values.sum() shouldBe 149_000L
        result.transfers shouldBe listOf(
            Transfer("수아", "동규", 41_750),
            Transfer("재훈", "동규", 9_500),
            Transfer("재훈", "민지", 26_250),
        )
    }

    "breakdown은 불참 차수도 0원으로 채워진다 — W2가 '2차 불참 → 0원'을 보여줘야 한다" {
        val result = SettlementInput(
            participants = participants("A", "B", "C", "D", "E"),
            rounds = listOf(
                round("r1", 1, total = 87_000, alcohol = 35_000, payerId = "A"),
                round("r2", 2, total = 42_000, alcohol = 30_000, payerId = "B"),
            ),
            attendance = attendance {
                drank("r1", "A", "B", "C", "D")
                sober("r1", "E")
                drank("r2", "A", "B", "C")
                absent("r2", "D", "E")
            },
        ).succeed()

        val e = result.breakdown.getValue("E")
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
