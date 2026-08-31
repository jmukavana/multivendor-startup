package com.jamlapps.meridian.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount held as a whole number of minor units in an explicit currency.
 *
 * <p>DAT-01 prohibits floating point representation of money and requires an explicit currency on
 * every amount. This type is the only representation of money in the system; {@code double},
 * {@code float} and bare {@code BigDecimal} must not appear in a monetary field.
 *
 * <p>Minor units means cents for KES: {@code Money.of(240_000, KES)} is KES 2,400.00. Amounts may
 * be negative, which a ledger contra entry and a refund both require.
 *
 * <p>There is deliberately no default rounding mode on {@link #percentage}. Commission, VAT and
 * holdback each round differently depending on what the figure is used for, and a default here
 * would silently pick one. The caller states the intent at the call site.
 */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money of(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    public Money negated() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    public Money times(long factor) {
        return new Money(Math.multiplyExact(minorUnits, factor), currency);
    }

    /**
     * Applies a rate to this amount, for example a 9% commission as {@code new BigDecimal("0.09")}.
     *
     * @param rate     the multiplier, not a percentage figure: 9% is 0.09, not 9
     * @param rounding how to resolve a fractional minor unit; state it explicitly, see the type note
     */
    public Money percentage(BigDecimal rate, RoundingMode rounding) {
        Objects.requireNonNull(rate, "rate must not be null");
        Objects.requireNonNull(rounding, "rounding must not be null");
        long result = BigDecimal.valueOf(minorUnits)
                .multiply(rate)
                .setScale(0, rounding)
                .longValueExact();
        return new Money(result, currency);
    }

    public boolean isZero() {
        return minorUnits == 0L;
    }

    public boolean isNegative() {
        return minorUnits < 0L;
    }

    public boolean isPositive() {
        return minorUnits > 0L;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    /**
     * Renders the amount for display and logging, for example {@code KES 2400.00}. Not a
     * serialization format; persistence stores {@link #minorUnits} and the currency code separately.
     */
    @Override
    public String toString() {
        return currency.getCurrencyCode() + " "
                + BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits()).toPlainString();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot combine " + currency.getCurrencyCode() + " with " + other.currency.getCurrencyCode());
        }
    }
}
