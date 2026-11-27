package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class TransferNotFoundException extends ApiException {

    public TransferNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TRANSFER_NOT_FOUND", "Transfer not found", "The transfer does not exist.");
    }
}
