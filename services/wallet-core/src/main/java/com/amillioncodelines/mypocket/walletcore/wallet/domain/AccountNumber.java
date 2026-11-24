package com.amillioncodelines.mypocket.walletcore.wallet.domain;

import java.util.random.RandomGenerator;

/** Public 10-digit wallet identifier: 9 random digits (first non-zero) plus a Luhn check digit. */
public final class AccountNumber {

    public static final int LENGTH = 10;

    private AccountNumber() {}

    public static String generate(RandomGenerator random) {
        var payload = new StringBuilder(LENGTH);
        payload.append(1 + random.nextInt(9));
        for (int i = 1; i < LENGTH - 1; i++) {
            payload.append(random.nextInt(10));
        }
        return payload.append(luhnCheckDigit(payload)).toString();
    }

    public static boolean isValid(String candidate) {
        if (candidate == null || !candidate.matches("[1-9][0-9]{" + (LENGTH - 1) + "}")) {
            return false;
        }
        var payload = candidate.substring(0, LENGTH - 1);
        return candidate.charAt(LENGTH - 1) - '0' == luhnCheckDigit(payload);
    }

    static int luhnCheckDigit(CharSequence payload) {
        int sum = 0;
        boolean doubleIt = true;
        for (int i = payload.length() - 1; i >= 0; i--) {
            int digit = payload.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return (10 - sum % 10) % 10;
    }
}
