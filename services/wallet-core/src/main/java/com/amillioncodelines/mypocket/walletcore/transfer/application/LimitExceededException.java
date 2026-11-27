package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class LimitExceededException extends ApiException {

    public LimitExceededException() {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "LIMIT_EXCEEDED",
                "Limit exceeded",
                "The amount exceeds the per-operation limit of the currency.");
    }
}
