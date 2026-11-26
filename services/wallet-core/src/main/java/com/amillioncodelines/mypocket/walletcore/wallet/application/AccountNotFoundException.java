package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "ACCOUNT_NOT_FOUND",
                "Account not found",
                "No wallet has this account number.");
    }
}
