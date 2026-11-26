package com.amillioncodelines.mypocket.walletcore.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1", "1.5", "1500.50", "9999999999999.99"})
    @DisplayName("M1: amounts with up to 2 decimals are accepted and kept with scale 2")
    void acceptsUpToTwoDecimals(String amount) {
        var money = Money.of(amount, Currency.ARS);

        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.amount()).isEqualByComparingTo(amount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "1.005", "1.500"})
    @DisplayName("M1: amounts with more than 2 decimals are rejected, never rounded")
    void rejectsMoreDecimals(String amount) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(amount, Currency.USD));
    }

    @Test
    @DisplayName("M1: amounts beyond 13 integer digits are rejected")
    void rejectsHugeAmounts() {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of("10000000000000.00", Currency.ARS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    @DisplayName("M2: amounts must be strictly greater than zero")
    void rejectsZeroAndNegative(String amount) {
        assertThatIllegalArgumentException().isThrownBy(() -> Money.of(amount, Currency.ARS));
    }
}
