package app.jeongsan.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * 정확한 유리수. 분자와 분모를 그대로 들고 다니며 **나눗셈을 실행하지 않는다.**
 *
 * `Double`은 물론 `BigDecimal`도 쓰지 않는 이유는 ADR-001에 있다. 요약하면
 * `1/3`은 어떤 진법에서도 유한 자릿수로 쓸 수 없어서 나누는 순간 반드시 정보가 버려지고,
 * 마지막 [ceilTo]가 그 티끌만한 오차를 한 단위(10원·100원)로 증폭시킨다.
 * 랜덤 정산 30,000건 중 210건(0.70%)에서 `BigDecimal`은 다른 금액을 냈다.
 * 정밀도를 100자리로 올려도 결과는 바뀌지 않는다 — 오차의 *크기*가 아니라 *존재*가 문제이기 때문이다.
 *
 * 실수 세계로 내려오는 지점은 [ceilTo] 하나뿐이다.
 */
class Rational private constructor(
    val numerator: BigInteger,
    /** 항상 양수로 정규화된다. */
    val denominator: BigInteger,
) : Comparable<Rational> {

    operator fun plus(other: Rational): Rational = of(
        numerator * other.denominator + other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun minus(other: Rational): Rational = of(
        numerator * other.denominator - other.numerator * denominator,
        denominator * other.denominator,
    )

    /**
     * 분모에 곱하기만 한다. **나눗셈을 실행하지 않는 것이 이 클래스의 존재 이유다.**
     */
    operator fun div(divisor: Int): Rational {
        require(divisor != 0) { "0으로 나눌 수 없다" }
        return of(numerator, denominator * BigInteger.valueOf(divisor.toLong()))
    }

    /**
     * `ceil(this / unit) * unit` — 올림이다. 반올림이 아니다.
     *
     * 정확한 유리수에서 정수로 내려오는 **유일한 지점**이다.
     */
    fun ceilTo(unit: Int): Long {
        require(unit > 0) { "반올림 단위는 양수여야 한다: $unit" }
        val u = BigInteger.valueOf(unit.toLong())
        return (ceilDiv(numerator, denominator * u) * u).longValueExact()
    }

    val isZero: Boolean get() = numerator.signum() == 0
    val signum: Int get() = numerator.signum()

    /**
     * 화면 표시 전용 문자열. **이 값을 계산에 되먹이지 말 것.**
     * 근거 화면(W2)에서 `6,666.67` 같은 중간값을 보여줄 때만 쓴다.
     */
    fun toDisplayString(decimals: Int = 2): String =
        BigDecimal(numerator)
            .divide(BigDecimal(denominator), decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    override fun compareTo(other: Rational): Int =
        (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String =
        if (denominator == BigInteger.ONE) numerator.toString() else "$numerator/$denominator"

    companion object {
        val ZERO: Rational = Rational(BigInteger.ZERO, BigInteger.ONE)

        fun of(value: Long): Rational = Rational(BigInteger.valueOf(value), BigInteger.ONE)

        fun of(numerator: BigInteger, denominator: BigInteger): Rational {
            require(denominator.signum() != 0) { "분모가 0이다" }
            var n = numerator
            var d = denominator
            if (d.signum() < 0) {
                n = n.negate()
                d = d.negate()
            }
            val g = n.gcd(d)
            return if (g > BigInteger.ONE) Rational(n / g, d / g) else Rational(n, d)
        }

        /** `b > 0` 가정. `BigInteger.divide`는 0 방향으로 절단하므로 나머지가 양수일 때만 보정한다. */
        private fun ceilDiv(a: BigInteger, b: BigInteger): BigInteger {
            val qr = a.divideAndRemainder(b)
            return if (qr[1].signum() > 0) qr[0] + BigInteger.ONE else qr[0]
        }
    }
}

fun Iterable<Rational>.sum(): Rational = fold(Rational.ZERO) { acc, r -> acc + r }
