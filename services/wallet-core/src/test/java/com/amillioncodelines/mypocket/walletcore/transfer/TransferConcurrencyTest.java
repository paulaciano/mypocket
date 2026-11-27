package com.amillioncodelines.mypocket.walletcore.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/** T7: concurrent transfers through the API never deadlock and never lose or create money. */
@IntegrationTest
class TransferConcurrencyTest {

    private static final int THREADS = 100;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    LoginService loginService;

    @Autowired
    Ledger ledger;

    @Autowired
    SystemWallets systemWallets;

    @Autowired
    WalletRepository wallets;

    @Autowired
    LedgerEntryRepository entries;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("T7: 100 concurrent opposite transfers between the same two wallets all complete and keep the total")
    void oppositeTransfers() throws Exception {
        var ana = customer();
        var bruno = customer();
        var requests = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < THREADS; i++) {
            var from = i % 2 == 0 ? ana : bruno;
            var to = i % 2 == 0 ? bruno : ana;
            requests.add(() -> transfer(from, to, "10.00", "key-" + UUID.randomUUID()));
        }

        var statuses = runConcurrently(requests);

        assertThat(statuses).containsOnly(201);
        assertThat(balance(ana)).isEqualByComparingTo("1000.00");
        assertThat(balance(bruno)).isEqualByComparingTo("1000.00");
        assertReconciled(List.of(ana, bruno));
    }

    @Test
    @DisplayName(
            "T7, L4: 100 concurrent transfers in every direction among five wallets keep the total and no balance goes negative")
    void transfersAmongManyWallets() throws Exception {
        var customers = new ArrayList<Customer>();
        for (int i = 0; i < 5; i++) {
            customers.add(customer());
        }
        var random = new Random(7);
        var requests = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < THREADS; i++) {
            var from = customers.get(random.nextInt(customers.size()));
            Customer to;
            do {
                to = customers.get(random.nextInt(customers.size()));
            } while (to == from);
            var target = to;
            var amount = (1 + random.nextInt(400)) + ".00";
            requests.add(() -> transfer(from, target, amount, "key-" + UUID.randomUUID()));
        }

        var statuses = runConcurrently(requests);

        // Some transfers may find their balance spent by others; none may fail for any other reason.
        assertThat(statuses).allMatch(status -> status == 201 || status == 422);
        var total = customers.stream().map(this::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("5000.00");
        assertThat(customers).allSatisfy(c -> assertThat(balance(c)).isNotNegative());
        assertReconciled(customers);
    }

    @Test
    @DisplayName("I5: concurrent transfers with the same key move money once; every caller gets the same transfer")
    void sameKeyConcurrently() throws Exception {
        var ana = customer();
        var bruno = customer();
        var key = "key-" + UUID.randomUUID();
        var token = loginService.login(ana.email(), "secret-pass").value();
        var requests = new ArrayList<Callable<MockHttpServletResponse>>();
        for (int i = 0; i < 20; i++) {
            requests.add(() -> mockMvc.perform(post("/api/v1/transfers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sourceWalletId": "%s", "destinationAccountNumber": "%s", "amount": "10.00"}
                                    """.formatted(
                                            ana.wallet().id(), bruno.wallet().accountNumber())))
                    .andReturn()
                    .getResponse());
        }

        var responses = new ArrayList<MockHttpServletResponse>();
        try (var pool = Executors.newFixedThreadPool(20)) {
            for (var result : pool.invokeAll(requests)) {
                responses.add(result.get());
            }
        }

        assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsOnly(201);
        var bodies = new ArrayList<String>();
        for (var response : responses) {
            bodies.add(response.getContentAsString());
        }
        assertThat(bodies).containsOnly(bodies.getFirst());
        assertThat(responses)
                .filteredOn(r -> r.getHeader("Idempotent-Replayed") == null)
                .hasSize(1);
        assertThat(balance(ana)).isEqualByComparingTo("990.00");
        assertThat(balance(bruno)).isEqualByComparingTo("1010.00");
    }

    private record Customer(String email, Wallet wallet) {}

    private Customer customer() {
        var email = "concurrency-" + UUID.randomUUID() + "@example.com";
        var wallet = registrationService.registerCustomer(email, "secret-pass").wallets().stream()
                .filter(w -> w.currency() == Currency.ARS)
                .findFirst()
                .orElseThrow();
        tx.executeWithoutResult(status -> ledger.move(
                TransactionType.CASH_IN,
                Money.of("1000.00", Currency.ARS),
                null,
                systemWallets.id(SystemPurpose.EXTERNAL, Currency.ARS),
                wallet.id()));
        return new Customer(email, wallet);
    }

    private int transfer(Customer from, Customer to, String amount, String key) throws Exception {
        var token = loginService.login(from.email(), "secret-pass").value();
        return mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceWalletId": "%s", "destinationAccountNumber": "%s", "amount": "%s"}
                                """.formatted(from.wallet().id(), to.wallet().accountNumber(), amount)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private static List<Integer> runConcurrently(List<Callable<Integer>> requests) throws Exception {
        var statuses = new ArrayList<Integer>();
        try (var pool = Executors.newFixedThreadPool(THREADS)) {
            for (var result : pool.invokeAll(requests)) {
                statuses.add(result.get());
            }
        }
        return statuses;
    }

    private BigDecimal balance(Customer customer) {
        return wallets.findById(customer.wallet().id()).orElseThrow().balance();
    }

    /** L3, L1: cached balances equal their entries, and every transaction of these wallets is balanced. */
    private void assertReconciled(List<Customer> customers) {
        for (var customer : customers) {
            assertThat(entries.computedBalance(customer.wallet().id())).isEqualByComparingTo(balance(customer));
        }
        var ids = customers.stream().map(c -> c.wallet().id()).toArray();
        var unbalanced =
                jdbc.queryForObject("""
                select count(*) from (
                    select transaction_id
                    from ledger_entries
                    where transaction_id in (select transaction_id from ledger_entries where wallet_id = any(?))
                    group by transaction_id
                    having count(*) <> 2
                        or sum(case when direction = 'DEBIT' then amount else -amount end) <> 0
                ) t""", Long.class, jdbc.execute((Connection c) -> c.createArrayOf("uuid", ids)));
        assertThat(unbalanced).isZero();
    }
}
