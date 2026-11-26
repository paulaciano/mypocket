package com.amillioncodelines.mypocket.walletcore.ledger.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One entry of a wallet history, with its transaction type and the other side of the movement. */
public interface HistoryRow {

    UUID getId();

    UUID getTransactionId();

    String getType();

    String getDirection();

    BigDecimal getAmount();

    Instant getCreatedAt();

    String getCounterpartAccountNumber();
}
