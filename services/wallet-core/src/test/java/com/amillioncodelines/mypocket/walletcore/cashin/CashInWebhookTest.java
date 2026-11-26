package com.amillioncodelines.mypocket.walletcore.cashin;

import static com.amillioncodelines.mypocket.walletcore.cashin.ProviderWebhook.signed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.AccountNumber;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import com.jayway.jsonpath.JsonPath;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class CashInWebhookTest {

    private static final String PATH = "/api/v1/webhooks/cash-ins";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    WalletRepository wallets;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MeterRegistry meters;

    @Test
    @DisplayName("CI1: a signed cash-in credits the wallet identified by its account number")
    void appliesSignedCashIn() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var reference = newReference();

        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "ARS", "10000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.accountNumber").value(wallet.accountNumber()))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.amount").value("10000.00"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(balance(wallet)).isEqualByComparingTo("10000.00");
    }

    @Test
    @DisplayName("S5: a missing, wrong or tampered signature is rejected before anything is applied")
    void rejectsInvalidSignatures() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var body = body(newReference(), wallet.accountNumber(), "ARS", "50.00");

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));
        mockMvc.perform(signed(PATH, body, Instant.now(), "another-provider-webhook-secret-of-32-bytes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));
        var tampered = signed(PATH, body).content(body.replace("50.00", "5000.00"));
        mockMvc.perform(tampered)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));

        assertThat(balance(wallet)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("S5: requests signed more than 5 minutes before or after now are rejected")
    void rejectsOutsideTimeWindow() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var body = body(newReference(), wallet.accountNumber(), "ARS", "50.00");
        var secret = ProviderWebhook.SECRET;

        mockMvc.perform(signed(PATH, body, Instant.now().minus(Duration.ofMinutes(6)), secret))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(signed(PATH, body, Instant.now().plus(Duration.ofMinutes(6)), secret))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(signed(PATH, body, Instant.now().minus(Duration.ofMinutes(4)), secret))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CI2, S6: repeating a cash-in returns the original result and credits the wallet once")
    void appliesReferenceOnce() throws Exception {
        var wallet = customerWallet(Currency.USD);
        var body = body(newReference(), wallet.accountNumber(), "USD", "25.50");

        var first = mockMvc.perform(signed(PATH, body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var repeated = mockMvc.perform(signed(PATH, body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<Object>read(repeated, "$")).isEqualTo(JsonPath.read(first, "$"));

        assertThat(balance(wallet)).isEqualByComparingTo("25.50");
    }

    @Test
    @DisplayName("CI2: concurrent deliveries of the same cash-in credit the wallet once")
    void concurrentDeliveriesApplyOnce() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var body = body(newReference(), wallet.accountNumber(), "ARS", "10.00");
        var deliveries = new ArrayList<Callable<Integer>>();
        for (int i = 0; i < 8; i++) {
            deliveries.add(() -> mockMvc.perform(signed(PATH, body))
                    .andReturn()
                    .getResponse()
                    .getStatus());
        }

        var statuses = new ArrayList<Integer>();
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (var result : pool.invokeAll(deliveries)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnly(201, 200);
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(balance(wallet)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CI3: the same reference with a different amount, currency or account is rejected")
    void rejectsReferenceWithDifferentData() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var other = customerWallet(Currency.ARS);
        var reference = newReference();
        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "ARS", "10.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "ARS", "11.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CASH_IN_REFERENCE_CONFLICT"));
        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "USD", "10.00")))
                .andExpect(status().isConflict());
        mockMvc.perform(signed(PATH, body(reference, other.accountNumber(), "ARS", "10.00")))
                .andExpect(status().isConflict());

        assertThat(balance(wallet)).isEqualByComparingTo("10.00");
        assertThat(balance(other)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CI4: the cash-in currency must match the wallet currency")
    void rejectsCurrencyMismatch() throws Exception {
        var wallet = customerWallet(Currency.ARS);

        mockMvc.perform(signed(PATH, body(newReference(), wallet.accountNumber(), "USD", "10.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
        assertThat(balance(wallet)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("CI1: an unknown account number is rejected")
    void rejectsUnknownAccount() throws Exception {
        String unknown;
        do {
            unknown = AccountNumber.generate(new Random());
        } while (wallets.existsByAccountNumber(unknown));

        mockMvc.perform(signed(PATH, body(newReference(), unknown, "ARS", "10.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("CI5, W3: a frozen or closed wallet cannot receive; a debit-blocked one can")
    void respectsWalletStatus() throws Exception {
        for (var status : new String[] {"FROZEN", "CLOSED"}) {
            var wallet = customerWallet(Currency.ARS);
            jdbc.update("update wallets set status = ? where id = ?", status, wallet.id());

            mockMvc.perform(signed(PATH, body(newReference(), wallet.accountNumber(), "ARS", "10.00")))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("WALLET_CANNOT_RECEIVE"));
            assertThat(balance(wallet)).isEqualByComparingTo("0.00");
        }
        var blocked = customerWallet(Currency.ARS);
        jdbc.update("update wallets set status = 'DEBIT_BLOCKED' where id = ?", blocked.id());

        mockMvc.perform(signed(PATH, body(newReference(), blocked.accountNumber(), "ARS", "10.00")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CI5: a rejected cash-in is evaluated again when the provider retries it")
    void retriesRejectedCashIn() throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var body = body(newReference(), wallet.accountNumber(), "ARS", "10.00");
        jdbc.update("update wallets set status = 'FROZEN' where id = ?", wallet.id());
        mockMvc.perform(signed(PATH, body)).andExpect(status().isUnprocessableContent());

        jdbc.update("update wallets set status = 'ACTIVE' where id = ?", wallet.id());

        mockMvc.perform(signed(PATH, body)).andExpect(status().isCreated());
        assertThat(balance(wallet)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CI6, M1: cash-ins have no business limit, only the 13 integer digits of M1")
    void noBusinessLimit() throws Exception {
        var wallet = customerWallet(Currency.USD);

        mockMvc.perform(signed(PATH, body(newReference(), wallet.accountNumber(), "USD", "9999999999999.99")))
                .andExpect(status().isCreated());
        mockMvc.perform(signed(PATH, body(newReference(), wallet.accountNumber(), "USD", "10000000000000.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    @Test
    @DisplayName("M1, M2: amounts are strings with up to 2 decimals and greater than zero")
    void validatesAmount() throws Exception {
        var account = customerWallet(Currency.ARS).accountNumber();
        for (var amount : new String[] {"\"10.001\"", "\"0.00\"", "\"-5.00\"", "\"1e3\"", "\" 10.00\"", "10.00"}) {
            var body = """
                    {"reference": "%s", "accountNumber": "%s", "currency": "ARS", "amount": %s}
                    """.formatted(newReference(), account, amount);
            mockMvc.perform(signed(PATH, body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("S7: references are provider identifiers without spaces or control characters")
    void validatesReference() throws Exception {
        var account = customerWallet(Currency.ARS).accountNumber();

        mockMvc.perform(signed(PATH, body("PRV 1\\nfake log line", account, "ARS", "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reference"));
    }

    @Test
    @DisplayName("S5: oversized webhook bodies are rejected")
    void rejectsLargeBodies() throws Exception {
        var body = "{\"reference\": \"" + "x".repeat(20_000) + "\"}";

        mockMvc.perform(signed(PATH, body)).andExpect(status().isContentTooLarge());
    }

    @Test
    @DisplayName("CI7: a rejected cash-in is logged with reference, account and reason, and counted by reason")
    void logsAndCountsRejections(CapturedOutput output) throws Exception {
        var wallet = customerWallet(Currency.ARS);
        var reference = newReference();
        var before = rejected("CURRENCY_MISMATCH");

        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "USD", "10.00")))
                .andExpect(status().isUnprocessableContent());

        assertThat(rejected("CURRENCY_MISMATCH")).isEqualTo(before + 1);
        assertThat(output)
                .contains("Cash-in rejected: reason=CURRENCY_MISMATCH, reference=" + reference + ", accountNumber="
                        + wallet.accountNumber());
    }

    private double rejected(String reason) {
        var counter =
                meters.find("mypocket.cashin.rejected").tag("reason", reason).counter();
        return counter == null ? 0 : counter.count();
    }

    private static String body(String reference, String accountNumber, String currency, String amount) {
        return """
                {"reference": "%s", "accountNumber": "%s", "currency": "%s", "amount": "%s"}
                """.formatted(reference, accountNumber, currency, amount);
    }

    private static String newReference() {
        return "PRV-" + UUID.randomUUID();
    }

    private Wallet customerWallet(Currency currency) {
        return registrationService
                .registerCustomer("cashin-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .stream()
                .filter(w -> w.currency() == currency)
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal balance(Wallet wallet) {
        return wallets.findById(wallet.id()).orElseThrow().balance();
    }
}
