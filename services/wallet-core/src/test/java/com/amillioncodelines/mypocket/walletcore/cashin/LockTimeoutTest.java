package com.amillioncodelines.mypocket.walletcore.cashin;

import static com.amillioncodelines.mypocket.walletcore.cashin.ProviderWebhook.signed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class LockTimeoutTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    WalletRepository wallets;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Test
    @DisplayName(
            "L7: an operation that cannot lock its wallet within 5 seconds answers 503, changes nothing and can be retried")
    void failsFastWhenWalletStaysLocked() throws Exception {
        var wallet = registrationService
                .registerCustomer("lock-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .getFirst();
        var body = """
                {"reference": "PRV-%s", "accountNumber": "%s", "currency": "%s", "amount": "10.00"}
                """.formatted(UUID.randomUUID(), wallet.accountNumber(), wallet.currency());
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);

        try (var pool = Executors.newSingleThreadExecutor()) {
            var holder = pool.submit(() -> tx.executeWithoutResult(status -> {
                jdbc.queryForList("select id from wallets where id = ? for update", wallet.id());
                locked.countDown();
                await(release);
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();

            var started = Instant.now();
            mockMvc.perform(signed("/api/v1/webhooks/cash-ins", body))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Retry-After", "1"))
                    .andExpect(jsonPath("$.code").value("SERVICE_BUSY"));
            assertThat(Duration.between(started, Instant.now()))
                    .isBetween(Duration.ofSeconds(4), Duration.ofSeconds(9));

            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
        }

        assertThat(wallets.findById(wallet.id()).orElseThrow().balance()).isEqualByComparingTo("0.00");
        mockMvc.perform(signed("/api/v1/webhooks/cash-ins", body)).andExpect(status().isCreated());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
