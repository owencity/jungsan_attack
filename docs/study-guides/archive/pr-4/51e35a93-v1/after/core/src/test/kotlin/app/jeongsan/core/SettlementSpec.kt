package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * `CALC_RULES_V2.md` §6의 예제와 핵심 조합을 옮긴 것이다.
 * **구현 결과가 다르면 구현이 틀린 것이다.**
 */
class SettlementSpec : StringSpec({

    "T1 · 기본 N빵 — 차수 총무가 1원 올림 잔액을 조정한다" {
        val ps = participants("A", "B", "C")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 10_000, alcohol = 0, payerId = ps.id("A"))),
            attendance = ps.attendance { sober(1L, "A", "B", "C") },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf("A" to 3_332L, "B" to 3_334L, "C" to 3_334L)
        result.amounts.values.sum() shouldBe 10_000L
        result.transfersByName(ps) shouldBe listOf("B→A 3334", "C→A 3334")
    }

    "T2 · 2차 불참 — 나누어떨어지면 총무 잔액 조정도 없다" {
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
            mapOf("A" to 11_666L, "B" to 11_667L, "C" to 11_667L, "D" to 5_000L)
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
        result.transfersByName(ps) shouldBe listOf(
            "B→A 10000", "C→A 10000", "D→A 10000", "A→B 10000", "D→B 10000",
        )
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
        result.transfersByName(ps) shouldBe listOf(
            "B→A 19150", "C→A 19150", "D→A 19150", "E→A 10400",
            "A→B 14000", "C→B 14000",
        )
    }

    "T6 · 동일 총무의 차수는 원부담을 합친 뒤 한 번만 1원 올림한다" {
        val ps = participants("A", "B", "C")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 10_000, alcohol = 0, payerId = ps.id("A")),
                round(2L, 2, total = 20_000, alcohol = 0, payerId = ps.id("A")),
            ),
            attendance = ps.attendance {
                sober(1L, "A", "B", "C")
                sober(2L, "A", "B", "C")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf("A" to 10_000L, "B" to 10_000L, "C" to 10_000L)
        result.transfersByName(ps) shouldBe listOf("B→A 10000", "C→A 10000")
    }

    "T7 · 1원 단위에서도 잔액 조정자가 음수면 거부한다" {
        val ps = participants("A", "B", "C")
        val errors = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 1, alcohol = 0, payerId = ps.id("A"))),
            attendance = ps.attendance { sober(1L, "A", "B", "C") },
        ).fail()

        errors.codes() shouldBe setOf(ErrorCode.NEGATIVE_ADJUSTED_AMOUNT)
    }

    "T7-b · 면제 총무면 비면제 부담자 중 한 명이 잔액을 조정한다" {
        val ps = participants("A", "B", "C", "D")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("D"))),
            attendance = ps.attendance {
                sober(1L, "A", "B", "C")
                exempt(1L, "D")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "A" to 13_332L, "B" to 13_334L, "C" to 13_334L, "D" to 0L,
        )
        result.recipients.getValue(ps.id("D")).adjustmentParticipantId shouldBe ps.id("A")
        result.transfersByName(ps) shouldBe
            listOf("A→D 13332", "B→D 13334", "C→D 13334")
    }

    "T8 · 면제자 — 면제자의 몫을 나머지가 나눠 낸다" {
        val ps = participants("동규", "민지", "재훈", "수아")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("동규"))),
            attendance = ps.attendance {
                sober(1L, "동규", "민지", "재훈")
                exempt(1L, "수아")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 13_332L, "민지" to 13_334L, "재훈" to 13_334L, "수아" to 0L,
        )
        result.amounts.values.sum() shouldBe 40_000L
        result.transfersByName(ps) shouldBe listOf("민지→동규 13334", "재훈→동규 13334")
    }

    "T9 · 수취인별 원금은 자기 부담과 들어오는 송금의 합과 같다" {
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

        result.recipients.values.forEach {
            it.ownShare + it.incomingTotal shouldBe it.paidTotal
            it.participantAmounts.values.sum() shouldBe it.paidTotal
        }
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
            "동규" to 26_332L, "민지" to 26_334L, "재훈" to 26_334L, "수아" to 12_000L,
        )
        result.amounts.values.sum() shouldBe 91_000L
    }

    "T11 · 전체 통합 — 차수별 면제 + 다중 총무" {
        val ps = participants("동규", "민지", "재훈", "수아", "지원")
        val result = SettlementInput(
            participants = ps,
            rounds = listOf(
                round(1L, 1, total = 87_000, alcohol = 35_000, payerId = ps.id("동규")),
                round(2L, 2, total = 42_000, alcohol = 30_000, payerId = ps.id("민지")),
            ),
            attendance = ps.attendance {
                drank(1L, "동규", "민지", "재훈", "수아")
                exempt(1L, "지원")
                drank(2L, "동규", "민지", "재훈")
                absent(2L, "수아", "지원")
            },
        ).succeed()

        result.amountsByName(ps) shouldBe mapOf(
            "동규" to 35_750L, "민지" to 35_750L, "재훈" to 35_750L,
            "수아" to 21_750L, "지원" to 0L,
        )
        result.amounts.values.sum() shouldBe 129_000L
    }

    "면제 총무가 결제하면 전액 돌려받는다 — CALC_RULES_V2 §3.4" {
        // 명세에 있는 규칙인데 T8·T11 어디에도 면제자가 결제자인 케이스가 없었다.
        // 코드 리뷰 F3에서 찾은 공백을 메우는 테스트다.
        val ps = participants("동규", "민지", "재훈", "수아")
        val result = SettlementInput(
            participants = ps,
            // 면제자인 수아가 결제한다
            rounds = listOf(round(1L, 1, total = 40_000, alcohol = 0, payerId = ps.id("수아"))),
            attendance = ps.attendance {
                sober(1L, "동규", "민지", "재훈")
                exempt(1L, "수아")
            },
        ).succeed()

        result.amountsByName(ps)["수아"] shouldBe 0L                        // 부담 0
        result.breakdown.getValue(ps.id("수아")).paidTotal shouldBe 40_000L  // 결제 40,000
        result.amounts.values.sum() shouldBe 40_000L
        result.breakdown.getValue(ps.id("수아")).rounds.single().exempt shouldBe true
        // 수아는 부담 0에 결제 40,000이므로 전액 회수한다.
        // 총무가 면제이므로 비면제 부담자 중 id가 가장 작은 동규가 잔액을 조정한다.
        result.transfersByName(ps) shouldBe
            listOf("동규→수아 13332", "민지→수아 13334", "재훈→수아 13334")
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
