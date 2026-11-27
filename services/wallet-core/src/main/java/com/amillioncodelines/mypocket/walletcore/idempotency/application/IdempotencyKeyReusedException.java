package com.amillioncodelines.mypocket.walletcore.idempotency.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {

    public IdempotencyKeyReusedException() {
        super(
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_REUSED",
                "Idempotency key reused",
                "This idempotency key was already used for a different request.");
    }
}
