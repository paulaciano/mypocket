package com.amillioncodelines.mypocket.walletcore.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.application.CurrencyMismatchException;
import com.amillioncodelines.mypocket.walletcore.ledger.application.InsufficientFundsException;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.EntryDirection;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerEntry;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerTransaction;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class LedgerTest {

    @Autowired
    Ledger ledger;

    @Autowired
    SystemWallets systemWallets;

    @Autowired
    WalletRepository wallets;

    @Autowired
    LedgerEntryRepository entries;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("L1: a movement is one transaction with a debit and a credit of the same amount")
    void movementIsBalanced() {
        var ars = customerWallet(Currency.ARS);
        var external = systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS);

        var transaction = move(TransactionType.CASH_IN, "1500.50", Currency.ARS, external, ars.id());

        var posted = entries.findByTransactionId(transaction.id());
        assertThat(posted).hasSize(2);
        assertThat(posted)
                .extracting(LedgerEntry::walletId, LedgerEntry::direction, LedgerEntry::amount)
                .containsExactlyInAnyOrder(
                        tuple(external, EntryDirection.DEBIT, new BigDecimal("1500.50")),
                        tuple(ars.id(), EntryDirection.CREDIT, new BigDecimal("1500.50")));
        assertThat(balance(ars.id())).isEqualByComparingTo("1500.50");
    }

    @Test
    @DisplayName("L2: transactions and entries cannot be updated or deleted, even directly in the database")
    void ledgerIsAppendOnly() {
        var ars = customerWallet(Currency.ARS);
        var transaction = move(
                TransactionType.CASH_IN,
                "10.00",
                Currency.ARS,
                systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS),
                ars.id());

        assertThatThrownBy(() ->
                        jdbc.update("update ledger_entries set amount = 1 where transaction_id = ?", transaction.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from ledger_entries where transaction_id = ?", transaction.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("update transactions set amount = 1 where id = ?", transaction.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from transactions where id = ?", transaction.id()))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("truncate ledger_entries, transactions"))
                .hasMessageContaining("append-only");
        assertThat(entries.findByTransactionId(transaction.id())).hasSize(2);
    }

    @Test
    @DisplayName("L4: a debit beyond the balance is rejected and nothing is written")
    void balanceNeverNegative() {
        var ars = customerWallet(Currency.ARS);
        var external = systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS);
        var inTransit = systemWallets.id(SystemPurpose.WITHDRAWALS_IN_TRANSIT, Currency.ARS);
        move(TransactionType.CASH_IN, "100.00", Currency.ARS, external, ars.id());

        assertThatThrownBy(() -> move(TransactionType.WITHDRAWAL, "100.01", Currency.ARS, ars.id(), inTransit))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balance(ars.id())).isEqualByComparingTo("100.00");
        assertThat(entries.computedBalance(ars.id())).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject(
                        "select count(*) from ledger_entries where wallet_id = ?", Long.class, inTransit))
                .isZero();
    }

    @Test
    @DisplayName("L4: concurrent debits never take a balance below zero")
    void concurrentDebitsNeverOverdraw() throws Exception {
        var ars = customerWallet(Currency.ARS);
        var external = systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS);
        move(TransactionType.CASH_IN, "50.00", Currency.ARS, external, ars.id());

        List<Callable<Boolean>> debits = Collections.nCopies(10, () -> {
            try {
                move(TransactionType.WITHDRAWAL, "10.00", Currency.ARS, ars.id(), external);
                return true;
            } catch (InsufficientFundsException e) {
                return false;
            }
        });
        int succeeded = 0;
        try (var pool = Executors.newFixedThreadPool(10)) {
            for (Future<Boolean> result : pool.invokeAll(debits)) {
                succeeded += result.get() ? 1 : 0;
            }
        }

        assertThat(succeeded).isEqualTo(5);
        assertThat(balance(ars.id())).isEqualByComparingTo("0.00");
        assertThat(entries.computedBalance(ars.id())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("M4: money only moves between wallets of the same currency")
    void sameCurrencyOnly() {
        var ars = customerWallet(Currency.ARS);
        var usdExternal = systemWallets.id(SystemPurpose.EXTERNAL, Currency.USD);
        var arsExternal = systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS);

        assertThatThrownBy(() -> move(TransactionType.CASH_IN, "10.00", Currency.USD, usdExternal, ars.id()))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> move(TransactionType.CASH_IN, "10.00", Currency.ARS, usdExternal, ars.id()))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> move(TransactionType.CASH_IN, "10.00", Currency.USD, arsExternal, ars.id()))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThat(balance(ars.id())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "L5: system wallets exist per purpose and currency, have no cached balance and are computed from entries")
    void systemWalletsAreComputed() {
        for (var currency : Currency.values()) {
            for (var purpose : SystemPurpose.values()) {
                var wallet =
                        wallets.findById(systemWallets.id(purpose, currency)).orElseThrow();
                assertThat(wallet.isCustomer()).isFalse();
                assertThat(wallet.balance()).isNull();
                assertThat(wallet.accountNumber()).isNull();
                assertThat(wallet.ownerId()).isNull();
            }
        }
        var external = systemWallets.id(SystemPurpose.EXTERNAL, Currency.USD);
        var before = entries.computedBalance(external);

        move(
                TransactionType.CASH_IN,
                "25.00",
                Currency.USD,
                external,
                customerWallet(Currency.USD).id());

        assertThat(entries.computedBalance(external)).isEqualByComparingTo(before.subtract(new BigDecimal("25.00")));
        assertThat(wallets.findById(external).orElseThrow().balance()).isNull();
    }

    @Test
    @DisplayName("L3: every customer wallet's cached balance equals the balance computed from its entries")
    void reconciliation() {
        var ars = customerWallet(Currency.ARS);
        var external = systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS);
        var inTransit = systemWallets.id(SystemPurpose.WITHDRAWALS_IN_TRANSIT, Currency.ARS);
        move(TransactionType.CASH_IN, "300.00", Currency.ARS, external, ars.id());
        move(TransactionType.WITHDRAWAL, "120.00", Currency.ARS, ars.id(), inTransit);
        move(TransactionType.WITHDRAWAL_REVERSAL, "20.00", Currency.ARS, inTransit, ars.id());
        move(TransactionType.WITHDRAWAL_SETTLEMENT, "100.00", Currency.ARS, inTransit, external);

        assertThat(balance(ars.id())).isEqualByComparingTo("200.00");
        var mismatches = jdbc.queryForList("""
                select w.id
                from wallets w
                left join ledger_entries e on e.wallet_id = w.id
                where w.kind = 'CUSTOMER'
                group by w.id, w.balance
                having w.balance <> coalesce(sum(case when e.direction = 'CREDIT' then e.amount else -e.amount end), 0)
                """, UUID.class);
        assertThat(mismatches).isEmpty();
        var unbalanced = jdbc.queryForList("""
                select transaction_id
                from ledger_entries
                group by transaction_id
                having count(*) < 2
                    or sum(case when direction = 'DEBIT' then amount else 0 end)
                       <> sum(case when direction = 'CREDIT' then amount else 0 end)
                """, UUID.class);
        assertThat(unbalanced).isEmpty();
        var total = jdbc.queryForObject(
                "select coalesce(sum(case when direction = 'CREDIT' then amount else -amount end), 0) from ledger_entries",
                BigDecimal.class);
        assertThat(total).isEqualByComparingTo("0");
    }

    private LedgerTransaction move(
            TransactionType type, String amount, Currency currency, UUID fromWalletId, UUID toWalletId) {
        return tx.execute(status -> ledger.move(type, Money.of(amount, currency), null, fromWalletId, toWalletId));
    }

    private Wallet customerWallet(Currency currency) {
        return registrationService
                .registerCustomer("ledger-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .stream()
                .filter(w -> w.currency() == currency)
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal balance(UUID walletId) {
        return wallets.findById(walletId).orElseThrow().balance();
    }
}
