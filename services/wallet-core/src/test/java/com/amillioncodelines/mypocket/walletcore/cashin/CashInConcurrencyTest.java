package com.amillioncodelines.mypocket.walletcore.cashin;

import static com.amillioncodelines.mypocket.walletcore.cashin.ProviderWebhook.signed;
import static org.assertj.core.api.Assertions.assertThat;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class CashInConcurrencyTest {

    private static final int CASH_INS = 20;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    WalletRepository wallets;

    @Autowired
    LedgerEntryRepository entries;

    @Test
    @DisplayName("L3: concurrent cash-ins to one wallet are all applied, in turn, and the balance matches the entries")
    void concurrentCashInsKeepBalance() throws Exception {
        var wallet = registrationService
                .registerCustomer("concurrent-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .getFirst();
        var deliveries = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < CASH_INS; i++) {
            var body = """
                    {"reference": "PRV-%s", "accountNumber": "%s", "currency": "%s", "amount": "10.00"}
                    """.formatted(UUID.randomUUID(), wallet.accountNumber(), wallet.currency());
            deliveries.add(() -> mockMvc.perform(signed("/api/v1/webhooks/cash-ins", body))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        var statuses = new ArrayList<Integer>();
        try (var pool = Executors.newFixedThreadPool(CASH_INS)) {
            for (var result : pool.invokeAll(deliveries)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnly(201);
        var balance = wallets.findById(wallet.id()).orElseThrow().balance();
        assertThat(balance).isEqualByComparingTo("200.00");
        assertThat(entries.computedBalance(wallet.id())).isEqualByComparingTo(balance);
    }
}
