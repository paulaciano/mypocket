package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class UnknownAccountNumberException extends ApiException {

    public UnknownAccountNumberException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "UNKNOWN_ACCOUNT_NUMBER",
                "Unknown account number",
                "No wallet has this account number.");
    }
}
