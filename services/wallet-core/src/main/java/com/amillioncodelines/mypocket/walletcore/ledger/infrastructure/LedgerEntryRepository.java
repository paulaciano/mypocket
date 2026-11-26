package com.amillioncodelines.mypocket.walletcore.ledger.infrastructure;

import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * L6: newest first. Every transaction has exactly two entries, so the counterpart is the other one; its
     * account number is shown only for transfers (system wallets have none).
     */
    @Query(
            nativeQuery = true,
            value = """
                    select e.id, e.transaction_id as transactionId, t.type, e.direction, e.amount,
                           e.created_at as createdAt,
                           case when t.type = 'TRANSFER' then cw.account_number end as counterpartAccountNumber
                    from ledger_entries e
                    join transactions t on t.id = e.transaction_id
                    join ledger_entries ce on ce.transaction_id = e.transaction_id and ce.id <> e.id
                    join wallets cw on cw.id = ce.wallet_id
                    where e.wallet_id = :walletId
                    order by e.created_at desc, e.id desc""",
            countQuery = "select count(*) from ledger_entries where wallet_id = :walletId")
    Page<HistoryRow> history(UUID walletId, Pageable pageable);
}
