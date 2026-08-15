package app.jeongsan.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.math.RoundingMode

class RationalSpec : StringSpec({

    /**
     * ADR-001 회귀 테스트.
     * **이 테스트가 깨지면 누군가 계산 엔진을 `BigDecimal`로 바꾼 것이다.**
     */
    "ADR-001 · BigDecimal이 틀리는 케이스를 유리수는 맞춘다" {
        // 3명 / 20,000원짜리 차수 3개 / 전원 참석 → 정확한 원부담은 딱 20,000원이다.
        val exact = (1..3).fold(Rational.ZERO) { acc, _ -> acc + Rational.of(20_000) / 3 }

        exact shouldBe Rational.of(20_000)
        exact.ceilTo(10) shouldBe 20_000L

        // 같은 계산을 BigDecimal HALF_UP 으로 하면 20,010원이 된다.
        // 정밀도를 100자리로 올려도 결과가 1원도 바뀌지 않는다 —
        // 오차의 *크기*가 아니라 *존재*가 문제이기 때문이다.
        listOf(12, 20, 34, 50, 100).forEach { scale ->
            var acc = BigDecimal.ZERO
            repeat(3) {
                acc = acc + BigDecimal(20_000).divide(BigDecimal(3), scale, RoundingMode.HALF_UP)
            }
            val ceiled = acc.divide(BigDecimal.TEN, 0, RoundingMode.CEILING).toLong() * 10
            ceiled shouldBe 20_010L
            ceiled shouldNotBe exact.ceilTo(10)
        }
    }

    "약분되어 정규화된다" {
        (Rational.of(10_000) / 4) shouldBe Rational.of(2_500)
        (Rational.of(1) / 3 + Rational.of(2) / 3) shouldBe Rational.of(1)
        (Rational.of(1) / 6 + Rational.of(1) / 3) shouldBe (Rational.of(1) / 2)
    }

    "ceilTo는 올림이다 — 반올림이 아니다" {
        (Rational.of(10_000) / 3).ceilTo(10) shouldBe 3_340L    // 3333.33 → 3340
        (Rational.of(10_000) / 3).ceilTo(100) shouldBe 3_400L   // 3333.33 → 3400
        Rational.of(3_330).ceilTo(10) shouldBe 3_330L           // 딱 떨어지면 그대로
        Rational.of(3_331).ceilTo(10) shouldBe 3_340L
    }

    "음수도 올림 방향이 일관된다" {
        (Rational.of(-1) / 2).ceilTo(10) shouldBe 0L      // ceil(-0.05) = 0
        Rational.of(-3_340).ceilTo(10) shouldBe -3_340L
        Rational.of(-3_331).ceilTo(10) shouldBe -3_330L   // ceil(-333.1) = -333
    }

    "비교는 통분해서 이뤄진다" {
        (Rational.of(1) / 3 > Rational.of(1) / 4) shouldBe true
        (Rational.of(2) / 6).compareTo(Rational.of(1) / 3) shouldBe 0
    }

    "표시용 문자열은 계산에 쓰지 않는다" {
        (Rational.of(20_000) / 3).toDisplayString() shouldBe "6666.67"
        (Rational.of(52_000) / 5).toDisplayString() shouldBe "10400"
    }
})
