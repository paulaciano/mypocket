package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class SameWalletException extends ApiException {

    public SameWalletException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "SAME_WALLET",
                "Same wallet",
                "Source and destination must be different wallets.");
    }
}
