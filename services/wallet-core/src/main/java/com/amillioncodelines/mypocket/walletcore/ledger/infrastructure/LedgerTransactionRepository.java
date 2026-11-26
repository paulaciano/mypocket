package com.amillioncodelines.mypocket.walletcore.ledger.infrastructure;

import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerTransaction;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Append-only: no update or delete methods (L2). */
public interface LedgerTransactionRepository extends Repository<LedgerTransaction, UUID> {

    LedgerTransaction save(LedgerTransaction transaction);
}
