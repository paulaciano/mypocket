package com.amillioncodelines.mypocket.walletcore.cashin.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class CashInReferenceConflictException extends ApiException {

    public CashInReferenceConflictException() {
        super(
                HttpStatus.CONFLICT,
                "CASH_IN_REFERENCE_CONFLICT",
                "Cash-in reference conflict",
                "A cash-in with this reference was already applied with different data.");
    }
}
