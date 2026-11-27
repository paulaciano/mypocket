package com.amillioncodelines.mypocket.walletcore.ledger.infrastructure;

import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerTransaction;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Append-only: no update or delete methods (L2). */
public interface LedgerTransactionRepository extends Repository<LedgerTransaction, UUID> {

    /** Flushes so a duplicate external reference fails here, translated to a DataIntegrityViolationException. */
    LedgerTransaction saveAndFlush(LedgerTransaction transaction);

    Optional<LedgerTransaction> findByTypeAndExternalReference(TransactionType type, String externalReference);

    Optional<LedgerTransaction> findByIdAndType(UUID id, TransactionType type);
}
