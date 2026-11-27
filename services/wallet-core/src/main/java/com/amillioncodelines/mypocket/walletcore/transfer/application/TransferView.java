package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import java.time.Instant;
import java.util.UUID;

/** A transfer as both sides see it (also the stored idempotent response). */
public record TransferView(
        UUID id,
        String sourceAccountNumber,
        String destinationAccountNumber,
        Currency currency,
        String amount,
        String description,
        Instant createdAt) {}
