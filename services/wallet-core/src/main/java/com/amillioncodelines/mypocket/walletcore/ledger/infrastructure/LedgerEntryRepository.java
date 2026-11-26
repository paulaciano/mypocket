package com.amillioncodelines.mypocket.walletcore.ledger.infrastructure;

import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Append-only: no update or delete methods (L2). */
public interface LedgerEntryRepository extends Repository<LedgerEntry, UUID> {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    @Query(nativeQuery = true, value = """
            select coalesce(sum(case when direction = 'CREDIT' then amount else -amount end), 0)
            from ledger_entries
            where wallet_id = :walletId""")
    BigDecimal computedBalance(UUID walletId);
}
