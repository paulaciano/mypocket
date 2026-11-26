package com.amillioncodelines.mypocket.walletcore.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerTransactionRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class LedgerTimestampTest {

    @Autowired
    WalletRepository wallets;

    @Autowired
    LedgerTransactionRepository transactions;

    @Autowired
    LedgerEntryRepository entries;

    @Autowired
    SystemWallets systemWallets;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    TransactionTemplate tx;

    @Test
    @DisplayName("CI2: a posted transaction has the same timestamp when it is read back (clocks with nanoseconds)")
    void timestampSurvivesStorage() {
        var clock = Clock.fixed(Instant.parse("2026-11-26T12:00:00.123456789Z"), ZoneOffset.UTC);
        var ledger = new Ledger(wallets, transactions, entries, clock);
        var wallet = registrationService
                .registerCustomer("timestamp-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .getFirst();
        var reference = "PRV-" + UUID.randomUUID();

        var posted = tx.execute(status -> ledger.move(
                TransactionType.CASH_IN,
                Money.of("1.00", wallet.currency()),
                reference,
                systemWallets.id(SystemPurpose.EXTERNAL, wallet.currency()),
                wallet.id()));

        var stored = ledger.findByReference(TransactionType.CASH_IN, reference)
                .orElseThrow()
                .transaction();
        assertThat(stored.createdAt()).isEqualTo(posted.createdAt());
    }
}
