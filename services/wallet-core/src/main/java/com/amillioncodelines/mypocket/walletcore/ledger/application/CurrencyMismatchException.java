package com.amillioncodelines.mypocket.walletcore.ledger.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class CurrencyMismatchException extends ApiException {

    public CurrencyMismatchException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "CURRENCY_MISMATCH",
                "Currency mismatch",
                "The amount and the wallets must be in the same currency.");
    }
}
