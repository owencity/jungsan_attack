package app.jeongsan.core.javaimpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** ADR-001: 나눗셈의 정보 손실이 마지막 올림을 바꾸지 않도록 분수 그대로 계산한다. */
public final class Rational implements Comparable<Rational> {
    public static final Rational ZERO = of(0);
    private final BigInteger numerator;
    private final BigInteger denominator;

    private Rational(BigInteger numerator, BigInteger denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public static Rational of(long value) {
        return new Rational(BigInteger.valueOf(value), BigInteger.ONE);
    }

    public static Rational of(BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) throw new IllegalArgumentException("분모가 0이다");
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        return new Rational(numerator.divide(gcd), denominator.divide(gcd));
    }

    public BigInteger numerator() { return numerator; }
    public BigInteger denominator() { return denominator; }
    public int signum() { return numerator.signum(); }
    public boolean isZero() { return signum() == 0; }

    public Rational plus(Rational other) {
        return of(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational minus(Rational other) {
        return of(numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Rational divide(int divisor) {
        if (divisor == 0) throw new IllegalArgumentException("0으로 나눌 수 없다");
        return of(numerator, denominator.multiply(BigInteger.valueOf(divisor)));
    }

    public long ceilTo(int unit) {
        if (unit <= 0) throw new IllegalArgumentException("올림 단위는 양수여야 한다: " + unit);
        BigInteger u = BigInteger.valueOf(unit);
        BigInteger[] qr = numerator.divideAndRemainder(denominator.multiply(u));
        // BigInteger는 0 방향으로 절단하므로 음수의 나머지는 보정하지 않는다.
        BigInteger ceil = qr[1].signum() > 0 ? qr[0].add(BigInteger.ONE) : qr[0];
        return ceil.multiply(u).longValueExact();
    }

    /** 표시 문자열은 계산 입력으로 되돌리지 않는다. */
    public String toDisplayString(int decimals) {
        return new BigDecimal(numerator).divide(new BigDecimal(denominator), decimals, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    public String toDisplayString() { return toDisplayString(2); }

    @Override public int compareTo(Rational other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }

    @Override public boolean equals(Object other) {
        return other instanceof Rational r && numerator.equals(r.numerator) && denominator.equals(r.denominator);
    }

    @Override public int hashCode() { return 31 * numerator.hashCode() + denominator.hashCode(); }

    @Override public String toString() {
        return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator;
    }
}
