package com.amillioncodelines.mypocket.walletcore.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.amillioncodelines.mypocket.walletcore.wallet.domain.AccountNumber;
import java.util.HashSet;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountNumberTest {

    @Test
    @DisplayName("W2: generated account numbers have 10 digits, no leading zero and a valid Luhn check digit")
    void generatedNumbersAreWellFormed() {
        var random = RandomGenerator.of("L64X128MixRandom");
        var seen = new HashSet<String>();
        for (int i = 0; i < 10_000; i++) {
            var number = AccountNumber.generate(random);
            assertThat(number).matches("[1-9][0-9]{9}");
            assertThat(AccountNumber.isValid(number)).isTrue();
            seen.add(number);
        }
        assertThat(seen).hasSizeGreaterThan(9_990);
    }

    @Test
    @DisplayName("W2: the check digit detects a mistyped digit")
    void checkDigitDetectsTypos() {
        assertThat(AccountNumber.isValid("4829103755")).isTrue();
        assertThat(AccountNumber.isValid("4829103756")).isFalse();
        assertThat(AccountNumber.isValid("4829013755")).isFalse();
        var valid = AccountNumber.generate(RandomGenerator.of("L64X128MixRandom"));
        var lastDigit = valid.charAt(9) - '0';
        var mistyped = valid.substring(0, 9) + ((lastDigit + 1) % 10);
        assertThat(AccountNumber.isValid(mistyped)).isFalse();
    }

    @Test
    @DisplayName("W2: rejects numbers with wrong length, letters or a leading zero")
    void rejectsMalformedNumbers() {
        assertThat(AccountNumber.isValid(null)).isFalse();
        assertThat(AccountNumber.isValid("123")).isFalse();
        assertThat(AccountNumber.isValid("12345678901")).isFalse();
        assertThat(AccountNumber.isValid("12345a7890")).isFalse();
        assertThat(AccountNumber.isValid("0000000000")).isFalse();
    }
}
