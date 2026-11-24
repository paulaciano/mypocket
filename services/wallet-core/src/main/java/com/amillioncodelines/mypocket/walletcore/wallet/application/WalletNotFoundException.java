package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends ApiException {

    public WalletNotFoundException() {
        super(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "Wallet not found", "The wallet does not exist.");
    }
}
