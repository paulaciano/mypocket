package com.amillioncodelines.mypocket.walletcore.cashin;

import static com.amillioncodelines.mypocket.walletcore.cashin.ProviderWebhook.signed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.rejection.domain.OperationType;
import com.amillioncodelines.mypocket.walletcore.rejection.infrastructure.RejectedOperationRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class RejectedCashInTest {

    private static final String PATH = "/api/v1/webhooks/cash-ins";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    RejectedOperationRepository rejected;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("R1: a cash-in rejected by a business rule is recorded with its data and reason")
    void recordsRejectedCashIn() throws Exception {
        var wallet = arsWallet();
        var reference = newReference();

        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "USD", "15.50")))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(signed(PATH, body(reference, wallet.accountNumber(), "USD", "15.50")))
                .andExpect(status().isUnprocessableContent());

        var records = rejected.findByProviderReference(reference);
        assertThat(records).hasSize(2);
        var record = records.getFirst();
        assertThat(record.type()).isEqualTo(OperationType.CASH_IN);
        assertThat(record.reason()).isEqualTo("CURRENCY_MISMATCH");
        assertThat(record.destinationAccountNumber()).isEqualTo(wallet.accountNumber());
        assertThat(record.amount()).isEqualByComparingTo("15.50");
        assertThat(record.currency()).isEqualTo(Currency.USD);
        assertThat(record.userId()).isNull();
        assertThat(record.sourceWalletId()).isNull();
        assertThat(record.createdAt()).isNotNull();
    }

    @Test
    @DisplayName(
            "R1: every business reason is recorded: unknown account, wallet that cannot receive, reference conflict")
    void recordsEveryBusinessReason() throws Exception {
        var unknown = newReference();
        mockMvc.perform(signed(PATH, body(unknown, "1000000009", "ARS", "1.00")));
        var frozen = arsWallet();
        jdbc.update("update wallets set status = 'FROZEN' where id = ?", frozen.id());
        var cannotReceive = newReference();
        mockMvc.perform(signed(PATH, body(cannotReceive, frozen.accountNumber(), "ARS", "1.00")));
        var wallet = arsWallet();
        var conflict = newReference();
        mockMvc.perform(signed(PATH, body(conflict, wallet.accountNumber(), "ARS", "1.00")))
                .andExpect(status().isCreated());
        mockMvc.perform(signed(PATH, body(conflict, wallet.accountNumber(), "ARS", "2.00")))
                .andExpect(status().isConflict());

        assertThat(rejected.findByProviderReference(unknown).getFirst().reason())
                .isEqualTo("UNKNOWN_ACCOUNT_NUMBER");
        assertThat(rejected.findByProviderReference(cannotReceive).getFirst().reason())
                .isEqualTo("WALLET_CANNOT_RECEIVE");
        assertThat(rejected.findByProviderReference(conflict)).singleElement().satisfies(record -> {
            assertThat(record.reason()).isEqualTo("CASH_IN_REFERENCE_CONFLICT");
            assertThat(record.amount()).isEqualByComparingTo("2.00");
        });
    }

    @Test
    @DisplayName("R3: accepted cash-ins, repetitions and requests rejected before the business rules are not recorded")
    void recordsOnlyBusinessRejections() throws Exception {
        var wallet = arsWallet();
        var accepted = newReference();
        mockMvc.perform(signed(PATH, body(accepted, wallet.accountNumber(), "ARS", "1.00")))
                .andExpect(status().isCreated());
        mockMvc.perform(signed(PATH, body(accepted, wallet.accountNumber(), "ARS", "1.00")))
                .andExpect(status().isOk());
        var unsigned = newReference();
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(unsigned, wallet.accountNumber(), "USD", "1.00")))
                .andExpect(status().isUnauthorized());
        var invalid = newReference();
        mockMvc.perform(signed(PATH, body(invalid, wallet.accountNumber(), "ARS", "0.001")))
                .andExpect(status().isBadRequest());

        assertThat(rejected.findByProviderReference(accepted)).isEmpty();
        assertThat(rejected.findByProviderReference(unsigned)).isEmpty();
        assertThat(rejected.findByProviderReference(invalid)).isEmpty();
    }

    @Test
    @DisplayName("R2: rejected operations cannot be updated or deleted, even directly in the database")
    void appendOnly() throws Exception {
        var reference = newReference();
        mockMvc.perform(signed(PATH, body(reference, "1000000009", "ARS", "1.00")));
        var id = rejected.findByProviderReference(reference).getFirst().id();

        assertThatThrownBy(() -> jdbc.update("update rejected_operations set reason = 'X' where id = ?", id))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from rejected_operations where id = ?", id))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("truncate rejected_operations")).hasMessageContaining("append-only");
    }

    private Wallet arsWallet() {
        return registrationService
                .registerCustomer("rejected-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .stream()
                .filter(w -> w.currency() == Currency.ARS)
                .findFirst()
                .orElseThrow();
    }

    private static String body(String reference, String accountNumber, String currency, String amount) {
        return """
                {"reference": "%s", "accountNumber": "%s", "currency": "%s", "amount": "%s"}
                """.formatted(reference, accountNumber, currency, amount);
    }

    private static String newReference() {
        return "PRV-" + UUID.randomUUID();
    }
}
