package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class WalletCannotSendException extends ApiException {

    public WalletCannotSendException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "WALLET_CANNOT_SEND",
                "Wallet cannot send money",
                "The wallet status does not allow sending money.");
    }
}
