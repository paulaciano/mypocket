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
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
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
    private final Clock clock;

    Ledger(
            WalletRepository wallets,
            LedgerTransactionRepository transactions,
            LedgerEntryRepository entries,
            Clock clock) {
        this.wallets = wallets;
        this.transactions = transactions;
        this.entries = entries;
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
        wallets.lockCustomerWallets(List.of(fromWalletId, toWalletId)).forEach(w -> involved.put(w.id(), w));
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

        var now = clock.instant();
        var transaction = transactions.save(new LedgerTransaction(type, money, externalReference, now));
        entries.save(new LedgerEntry(transaction.id(), from.id(), EntryDirection.DEBIT, money.amount(), now));
        entries.save(new LedgerEntry(transaction.id(), to.id(), EntryDirection.CREDIT, money.amount(), now));
        return transaction;
    }

    private Wallet existing(UUID walletId) {
        return wallets.findById(walletId).orElseThrow(() -> new IllegalArgumentException("Unknown wallet " + walletId));
    }
}
