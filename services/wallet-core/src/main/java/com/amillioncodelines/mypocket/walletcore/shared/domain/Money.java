package com.amillioncodelines.mypocket.walletcore.shared.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** A strictly positive amount with at most 2 decimals in one currency (M1, M2, ADR-0009). */
public record Money(BigDecimal amount, Currency currency) {

    public static final int SCALE = 2;
    /**
     * Keeps every amount within the NUMERIC(19,2) columns. A balance can still outgrow them after about 10,000
     * maximum-size cash-ins to one wallet (accepted limitation, see docs/05-security.md).
     */
    public static final int MAX_INTEGER_DIGITS = 13;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (!isAcceptable(amount)) {
            throw new IllegalArgumentException("Amount must be positive, with at most " + SCALE + " decimals and "
                    + MAX_INTEGER_DIGITS + " integer digits");
        }
        amount = amount.setScale(SCALE);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /** Never rounds: an amount with more than 2 decimals is not acceptable. */
    public static boolean isAcceptable(BigDecimal amount) {
        return amount.signum() > 0
                && amount.scale() <= SCALE
                && amount.precision() - amount.scale() <= MAX_INTEGER_DIGITS;
    }
}
