package com.amillioncodelines.mypocket.walletcore.ledger.api;

import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.HistoryRow;
import java.time.Instant;
import java.util.UUID;

record EntryResponse(
        UUID id,
        UUID transactionId,
        String type,
        String direction,
        String amount,
        Instant createdAt,
        String counterpartAccountNumber) {

    static EntryResponse from(HistoryRow row) {
        return new EntryResponse(
                row.getId(),
                row.getTransactionId(),
                row.getType(),
                row.getDirection(),
                row.getAmount().toPlainString(),
                row.getCreatedAt(),
                row.getCounterpartAccountNumber());
    }
}
