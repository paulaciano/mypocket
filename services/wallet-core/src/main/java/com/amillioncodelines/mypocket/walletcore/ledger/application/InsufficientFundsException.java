package com.amillioncodelines.mypocket.walletcore.ledger.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "INSUFFICIENT_FUNDS",
                "Insufficient funds",
                "Wallet balance does not cover the requested amount.");
    }
}
