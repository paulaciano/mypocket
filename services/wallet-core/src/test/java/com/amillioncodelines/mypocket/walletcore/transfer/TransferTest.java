package com.amillioncodelines.mypocket.walletcore.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.rejection.domain.OperationType;
import com.amillioncodelines.mypocket.walletcore.rejection.infrastructure.RejectedOperationRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService.Registration;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class TransferTest {

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
    RejectedOperationRepository rejected;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("T1, T6: a customer sends money from one of their wallets to an account number; both balances change")
    void sendsMoney() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "100.00");

        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "30.50", "Dinner")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.sourceAccountNumber").value(ars(ana).accountNumber()))
                .andExpect(
                        jsonPath("$.destinationAccountNumber").value(ars(bruno).accountNumber()))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.amount").value("30.50"))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(balance(ars(ana))).isEqualByComparingTo("69.50");
        assertThat(balance(ars(bruno))).isEqualByComparingTo("30.50");
    }

    @Test
    @DisplayName("T1: an unknown account number is rejected")
    void rejectsUnknownAccount() throws Exception {
        var ana = register();
        fund(ars(ana), "10.00");

        send(ana, newKey(), ars(ana).id(), "1000000009", "1.00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_ACCOUNT_NUMBER"));
        assertThat(balance(ars(ana))).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("T2, M4: source and destination must be different wallets of the same currency")
    void rejectsSameWalletAndOtherCurrency() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");

        send(ana, newKey(), ars(ana).id(), ars(ana).accountNumber(), "1.00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SAME_WALLET"));
        send(ana, newKey(), ars(ana).id(), usd(bruno).accountNumber(), "1.00", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
        send(ana, newKey(), ars(ana).id(), usd(ana).accountNumber(), "1.00", null)
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
        assertThat(balance(ars(ana))).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("W10: a customer cannot send from a wallet they do not own")
    void rejectsSomeoneElsesWallet() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(bruno), "10.00");

        send(ana, newKey(), ars(bruno).id(), ars(ana).accountNumber(), "1.00", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
        assertThat(balance(ars(bruno))).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("T3, W3: the source must be able to send and the destination able to receive")
    void respectsWalletStatus() throws Exception {
        for (var status : new String[] {"DEBIT_BLOCKED", "FROZEN", "CLOSED"}) {
            var ana = register();
            var bruno = register();
            fund(ars(ana), "10.00");
            setStatus(ars(ana), status);

            send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "1.00", null)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("WALLET_CANNOT_SEND"));
        }
        for (var status : new String[] {"FROZEN", "CLOSED"}) {
            var ana = register();
            var bruno = register();
            fund(ars(ana), "10.00");
            setStatus(ars(bruno), status);

            send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "1.00", null)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("WALLET_CANNOT_RECEIVE"));
            assertThat(balance(ars(ana))).isEqualByComparingTo("10.00");
        }
        var ana = register();
        var blocked = register();
        fund(ars(ana), "10.00");
        setStatus(ars(blocked), "DEBIT_BLOCKED");

        send(ana, newKey(), ars(ana).id(), ars(blocked).accountNumber(), "1.00", null)
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("T4, T6: the balance must cover the amount; otherwise neither balance changes")
    void rejectsInsufficientFunds() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");

        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "10.01", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        assertThat(balance(ars(ana))).isEqualByComparingTo("10.00");
        assertThat(balance(ars(bruno))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("T5: the amount cannot exceed the per-operation limit of the currency")
    void respectsPerOperationLimit() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "2000000.00");
        fund(usd(ana), "10000.00");

        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "1000000.01", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));
        send(ana, newKey(), usd(ana).id(), usd(bruno).accountNumber(), "5000.01", null)
                .andExpect(jsonPath("$.code").value("LIMIT_EXCEEDED"));
        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "1000000.00", null)
                .andExpect(status().isCreated());
        send(ana, newKey(), usd(ana).id(), usd(bruno).accountNumber(), "5000.00", null)
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("T8: the description is optional, up to 140 characters and without control characters")
    void validatesDescription() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");
        var destination = ars(bruno).accountNumber();

        send(ana, newKey(), ars(ana).id(), destination, "1.00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()));
        send(ana, newKey(), ars(ana).id(), destination, "1.00", "x".repeat(140)).andExpect(status().isCreated());
        send(ana, newKey(), ars(ana).id(), destination, "1.00", "x".repeat(141))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"));
        send(ana, newKey(), ars(ana).id(), destination, "1.00", "line\\nbreak").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("I1: a missing or invalid Idempotency-Key is rejected")
    void requiresIdempotencyKey() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");
        var body = body(ars(ana).id(), ars(bruno).accountNumber(), "1.00", null);

        mockMvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
        send(ana, "x".repeat(101), ars(ana).id(), ars(bruno).accountNumber(), "1.00", null)
                .andExpect(status().isBadRequest());
        send(ana, "with space", ars(ana).id(), ars(bruno).accountNumber(), "1.00", null)
                .andExpect(status().isBadRequest());
        send(ana, "x".repeat(100), ars(ana).id(), ars(bruno).accountNumber(), "1.00", null)
                .andExpect(status().isCreated());
        assertThat(balance(ars(ana))).isEqualByComparingTo("9.00");
    }

    @Test
    @DisplayName("I3: repeating a transfer returns the original response, marked as replayed, and moves no money")
    void replaysTransfer() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");
        var key = newKey();

        var first = send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "3.00", "Taxi")
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var repeated = send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "3.0", "Taxi")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(repeated).isEqualTo(first);
        assertThat(balance(ars(ana))).isEqualByComparingTo("7.00");
        assertThat(balance(ars(bruno))).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("I4: reusing a key for a different transfer is rejected")
    void rejectsKeyReuse() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "10.00");
        var key = newKey();
        send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "3.00", null).andExpect(status().isCreated());

        send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "4.00", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        assertThat(balance(ars(ana))).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("I6: a transfer rejected by a business rule can be retried with the same key")
    void retriesRejectedTransfer() throws Exception {
        var ana = register();
        var bruno = register();
        var key = newKey();
        send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "5.00", null)
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        fund(ars(ana), "5.00");

        send(ana, key, ars(ana).id(), ars(bruno).accountNumber(), "5.00", null)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        assertThat(balance(ars(bruno))).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName(
            "R1, R3: rejected transfers are recorded with their reason; requests rejected before the rules are not")
    void recordsRejectedTransfers() throws Exception {
        var ana = register();
        var bruno = register();
        fund(ars(ana), "1.00");
        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "2.00", "Secret note")
                .andExpect(status().isUnprocessableContent());
        var unknownWallet = UUID.randomUUID();
        send(ana, newKey(), unknownWallet, ars(bruno).accountNumber(), "1.00", null)
                .andExpect(status().isNotFound());
        send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "0.001", null)
                .andExpect(status().isBadRequest());

        var records = rejected.findByUserId(ana.user().id());
        assertThat(records).hasSize(2);
        assertThat(records)
                .anySatisfy(record -> {
                    assertThat(record.type()).isEqualTo(OperationType.TRANSFER);
                    assertThat(record.reason()).isEqualTo("INSUFFICIENT_FUNDS");
                    assertThat(record.sourceWalletId()).isEqualTo(ars(ana).id());
                    assertThat(record.destinationAccountNumber())
                            .isEqualTo(ars(bruno).accountNumber());
                    assertThat(record.amount()).isEqualByComparingTo("2.00");
                    assertThat(record.currency()).isEqualTo(Currency.ARS);
                })
                .anySatisfy(record -> {
                    assertThat(record.reason()).isEqualTo("WALLET_NOT_FOUND");
                    assertThat(record.sourceWalletId()).isEqualTo(unknownWallet);
                    assertThat(record.currency()).isNull();
                });
        assertThat(jdbc.queryForObject(
                        "select count(*) from rejected_operations where cast(rejected_operations as text) like '%Secret note%'",
                        Long.class))
                .isZero();
    }

    @Test
    @DisplayName("S4: a transfer is visible to its sender and recipient only")
    void transferVisibility() throws Exception {
        var ana = register();
        var bruno = register();
        var carla = register();
        fund(ars(ana), "10.00");
        var created = send(ana, newKey(), ars(ana).id(), ars(bruno).accountNumber(), "2.00", "Lunch")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(created, "$.id");

        for (var viewer : new Registration[] {ana, bruno}) {
            mockMvc.perform(get("/api/v1/transfers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.sourceAccountNumber").value(ars(ana).accountNumber()))
                    .andExpect(jsonPath("$.destinationAccountNumber")
                            .value(ars(bruno).accountNumber()))
                    .andExpect(jsonPath("$.amount").value("2.00"))
                    .andExpect(jsonPath("$.description").value("Lunch"));
        }
        mockMvc.perform(get("/api/v1/transfers/" + id).header(HttpHeaders.AUTHORIZATION, bearer(carla)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
        var cashIn = fund(ars(carla), "1.00");
        mockMvc.perform(get("/api/v1/transfers/" + cashIn).header(HttpHeaders.AUTHORIZATION, bearer(carla)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/transfers/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(carla)))
                .andExpect(status().isNotFound());
    }

    private ResultActions send(
            Registration user, String key, UUID sourceWalletId, String destination, String amount, String description)
            throws Exception {
        return mockMvc.perform(post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(sourceWalletId, destination, amount, description)));
    }

    private static String body(UUID sourceWalletId, String destination, String amount, String description) {
        var descriptionJson = description == null ? "null" : "\"" + description + "\"";
        return """
                {"sourceWalletId": "%s", "destinationAccountNumber": "%s", "amount": "%s", "description": %s}
                """.formatted(sourceWalletId, destination, amount, descriptionJson);
    }

    private UUID fund(Wallet wallet, String amount) {
        return tx.execute(status -> ledger.move(
                        TransactionType.CASH_IN,
                        Money.of(amount, wallet.currency()),
                        null,
                        systemWallets.id(SystemPurpose.EXTERNAL, wallet.currency()),
                        wallet.id()))
                .id();
    }

    private void setStatus(Wallet wallet, String status) {
        jdbc.update("update wallets set status = ? where id = ?", status, wallet.id());
    }

    private Registration register() {
        return registrationService.registerCustomer("transfer-" + UUID.randomUUID() + "@example.com", "secret-pass");
    }

    private static Wallet ars(Registration registration) {
        return wallet(registration, Currency.ARS);
    }

    private static Wallet usd(Registration registration) {
        return wallet(registration, Currency.USD);
    }

    private static Wallet wallet(Registration registration, Currency currency) {
        return registration.wallets().stream()
                .filter(w -> w.currency() == currency)
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal balance(Wallet wallet) {
        return wallets.findById(wallet.id()).orElseThrow().balance();
    }

    private String bearer(Registration registration) {
        return "Bearer "
                + loginService.login(registration.user().email(), "secret-pass").value();
    }

    private static String newKey() {
        return "key-" + UUID.randomUUID();
    }
}
