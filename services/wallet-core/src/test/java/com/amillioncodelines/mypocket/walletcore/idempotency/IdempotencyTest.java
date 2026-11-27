package com.amillioncodelines.mypocket.walletcore.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.Idempotency;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.Idempotency.Response;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.IdempotencyKeyReusedException;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.IdempotentOperation;
import com.amillioncodelines.mypocket.walletcore.ledger.application.InsufficientFundsException;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService.Registration;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class IdempotencyTest {

    @Autowired
    Idempotency idempotency;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    Ledger ledger;

    @Autowired
    SystemWallets systemWallets;

    @Autowired
    WalletRepository wallets;

    @Autowired
    JdbcTemplate jdbc;

    private final AtomicInteger executions = new AtomicInteger();

    @Test
    @DisplayName("I3: repeating a request with the same key returns the original response and moves no money")
    void replaysOriginalResponse() {
        var user = register();
        var key = newKey();

        var first = credit(user, key, "10.00");
        var second = credit(user, key, "10.00");

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.status()).isEqualTo(first.status()).isEqualTo(201);
        assertThat(second.body()).isEqualTo(first.body());
        assertThat(executions).hasValue(1);
        assertThat(balance(user)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("I2: keys are scoped per user")
    void keysArePerUser() {
        var ana = register();
        var bruno = register();
        var key = newKey();

        assertThat(credit(ana, key, "10.00").replayed()).isFalse();
        assertThat(credit(bruno, key, "10.00").replayed()).isFalse();

        assertThat(executions).hasValue(2);
        assertThat(balance(ana)).isEqualByComparingTo("10.00");
        assertThat(balance(bruno)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("I4: reusing a key with a different request or for another operation is rejected")
    void rejectsReuseWithOtherRequest() {
        var user = register();
        var key = newKey();
        credit(user, key, "10.00");

        assertThatThrownBy(() -> credit(user, key, "11.00")).isInstanceOf(IdempotencyKeyReusedException.class);
        assertThatThrownBy(() -> idempotency.execute(
                        user.user().id(),
                        key,
                        IdempotentOperation.WITHDRAWAL,
                        Map.of("amount", "10.00"),
                        HttpStatus.ACCEPTED,
                        () -> Map.of("id", "x")))
                .isInstanceOf(IdempotencyKeyReusedException.class);

        assertThat(executions).hasValue(1);
        assertThat(balance(user)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("I5: concurrent requests with the same key result in a single operation; the others get its response")
    void concurrentRequestsRunOnce() throws Exception {
        var user = register();
        var key = newKey();
        var requests = new ArrayList<Callable<Response>>();
        for (int i = 0; i < 10; i++) {
            requests.add(() -> credit(user, key, "10.00"));
        }

        var responses = new ArrayList<Response>();
        try (var pool = Executors.newFixedThreadPool(10)) {
            for (var result : pool.invokeAll(requests)) {
                responses.add(result.get());
            }
        }

        assertThat(executions).hasValue(1);
        assertThat(responses).filteredOn(r -> !r.replayed()).hasSize(1);
        assertThat(responses)
                .extracting(Response::body)
                .containsOnly(responses.getFirst().body());
        assertThat(balance(user)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("I6: a request rejected by a business rule leaves no record and is evaluated again on retry")
    void rejectionsAreNotStored() {
        var user = register();
        var key = newKey();

        assertThatThrownBy(() -> idempotency.execute(
                        user.user().id(),
                        key,
                        IdempotentOperation.TRANSFER,
                        Map.of("amount", "10.00"),
                        HttpStatus.CREATED,
                        () -> {
                            executions.incrementAndGet();
                            throw new InsufficientFundsException();
                        }))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(jdbc.queryForObject(
                        "select count(*) from idempotency_records where user_id = ?",
                        Long.class,
                        user.user().id()))
                .isZero();

        assertThat(credit(user, key, "10.00").replayed()).isFalse();
        assertThat(executions).hasValue(2);
    }

    @Test
    @DisplayName("I7: idempotency records do not expire")
    void recordsDoNotExpire() {
        var user = register();
        var key = newKey();
        var first = credit(user, key, "10.00");
        jdbc.update(
                "update idempotency_records set created_at = now() - interval '5 years' where user_id = ?",
                user.user().id());

        var later = credit(user, key, "10.00");

        assertThat(later.replayed()).isTrue();
        assertThat(later.body()).isEqualTo(first.body());
        assertThat(executions).hasValue(1);
    }

    /** A stand-in money operation: credits the user's first wallet and answers with the transaction id. */
    private Response credit(Registration user, String key, String amount) {
        var wallet = user.wallets().getFirst();
        var money = Money.of(amount, wallet.currency());
        return idempotency.execute(
                user.user().id(),
                key,
                IdempotentOperation.TRANSFER,
                Map.of("amount", amount),
                HttpStatus.CREATED,
                () -> {
                    executions.incrementAndGet();
                    var transaction = ledger.move(
                            TransactionType.CASH_IN,
                            money,
                            null,
                            systemWallets.id(SystemPurpose.EXTERNAL, wallet.currency()),
                            wallet.id());
                    return Map.of(
                            "id",
                            transaction.id().toString(),
                            "amount",
                            money.amount().toPlainString());
                });
    }

    private Registration register() {
        return registrationService.registerCustomer("idem-" + UUID.randomUUID() + "@example.com", "secret-pass");
    }

    private BigDecimal balance(Registration user) {
        Wallet wallet = user.wallets().getFirst();
        return wallets.findById(wallet.id()).orElseThrow().balance();
    }

    private static String newKey() {
        return "key-" + UUID.randomUUID();
    }
}
