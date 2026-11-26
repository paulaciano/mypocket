package com.amillioncodelines.mypocket.walletcore.cashin.api;

import com.amillioncodelines.mypocket.walletcore.cashin.application.CashIns;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import java.time.Instant;
import java.util.UUID;

record CashInResponse(
        UUID id, String reference, String accountNumber, Currency currency, String amount, Instant createdAt) {

    static CashInResponse of(CashIns.Result result) {
        return new CashInResponse(
                result.transactionId(),
                result.reference(),
                result.accountNumber(),
                result.money().currency(),
                result.money().amount().toPlainString(),
                result.createdAt());
    }
}
