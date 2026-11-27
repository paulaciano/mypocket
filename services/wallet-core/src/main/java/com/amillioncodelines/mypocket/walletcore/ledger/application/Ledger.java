package com.amillioncodelines.mypocket.walletcore.ledger.application;

import com.amillioncodelines.mypocket.walletcore.ledger.domain.EntryDirection;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerEntry;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerTransaction;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerTransactionRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The only component that writes ledger entries and changes balances (ADR-0003). */
@Service
public class Ledger {

    private final WalletRepository wallets;
    private final LedgerTransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final EntityManager entityManager;
    private final Clock clock;

    Ledger(
            WalletRepository wallets,
            LedgerTransactionRepository transactions,
            LedgerEntryRepository entries,
            EntityManager entityManager,
            Clock clock) {
        this.wallets = wallets;
        this.transactions = transactions;
        this.entries = entries;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    /**
     * Moves money from one wallet to another as one balanced transaction (L1): a debit on {@code fromWalletId}
     * and a credit on {@code toWalletId}. Wallet status rules belong to the caller; the ledger only guarantees
     * that money is neither lost nor created and that customer balances never go negative (L4). Must run inside
     * the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerTransaction move(
            TransactionType type, Money money, String externalReference, UUID fromWalletId, UUID toWalletId) {
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Source and destination must be different wallets");
        }
        var involved = new HashMap<UUID, Wallet>();
        for (var wallet : wallets.lockCustomerWallets(List.of(fromWalletId, toWalletId))) {
            // The lock query hands back a wallet the caller may have loaded earlier, with a stale balance.
            entityManager.refresh(wallet);
            involved.put(wallet.id(), wallet);
        }
        var from = involved.computeIfAbsent(fromWalletId, this::existing);
        var to = involved.computeIfAbsent(toWalletId, this::existing);

        if (from.currency() != money.currency() || to.currency() != money.currency()) {
            throw new CurrencyMismatchException();
        }
        if (from.isCustomer()) {
            if (!from.covers(money.amount())) {
                throw new InsufficientFundsException();
            }
            from.debit(money.amount());
        }
        if (to.isCustomer()) {
            to.credit(money.amount());
        }

        // PostgreSQL keeps microseconds: the result returned now must equal the one read back later (CI2).
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var transaction = transactions.saveAndFlush(new LedgerTransaction(type, money, externalReference, now));
        entries.save(new LedgerEntry(transaction.id(), from.id(), EntryDirection.DEBIT, money.amount(), now));
        entries.save(new LedgerEntry(transaction.id(), to.id(), EntryDirection.CREDIT, money.amount(), now));
        return transaction;
    }

    /** A transaction already posted with this external reference, with the wallet it credited. */
    @Transactional(readOnly = true)
    public Optional<Posted> findByReference(TransactionType type, String externalReference) {
        return transactions
                .findByTypeAndExternalReference(type, externalReference)
                .map(transaction -> {
                    var credited = entries.findByTransactionId(transaction.id()).stream()
                            .filter(entry -> entry.direction() == EntryDirection.CREDIT)
                            .findFirst()
                            .orElseThrow()
                            .walletId();
                    return new Posted(transaction, credited);
                });
    }

    public record Posted(LedgerTransaction transaction, UUID creditedWalletId) {}

    private Wallet existing(UUID walletId) {
        return wallets.findById(walletId).orElseThrow(() -> new IllegalArgumentException("Unknown wallet " + walletId));
    }
}
