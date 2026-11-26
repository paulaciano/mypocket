package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class WalletCannotReceiveException extends ApiException {

    public WalletCannotReceiveException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "WALLET_CANNOT_RECEIVE",
                "Wallet cannot receive money",
                "The wallet status does not allow receiving money.");
    }
}
