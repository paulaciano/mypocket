package com.amillioncodelines.mypocket.walletcore.ledger;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.LedgerTransaction;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService.Registration;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class WalletHistoryTest {

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
    TransactionTemplate tx;

    @Test
    @DisplayName("L6: the history lists entries newest first, with type, direction and the transfer counterpart")
    void listsEntriesNewestFirst() throws Exception {
        var ana = register();
        var bruno = register();
        var anaArs = ars(ana);
        var brunoArs = ars(bruno);
        var cashIn = cashIn(anaArs, "100.00");
        var transfer = move(TransactionType.TRANSFER, "30.00", anaArs.id(), brunoArs.id());
        var withdrawal = move(
                TransactionType.WITHDRAWAL,
                "20.00",
                anaArs.id(),
                systemWallets.id(SystemPurpose.WITHDRAWALS_IN_TRANSIT, Currency.ARS));

        mockMvc.perform(get(entriesOf(anaArs)).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].transactionId")
                        .value(withdrawal.id().toString()))
                .andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value("20.00"))
                .andExpect(jsonPath("$.content[0].counterpartAccountNumber").value(nullValue()))
                .andExpect(jsonPath("$.content[1].transactionId")
                        .value(transfer.id().toString()))
                .andExpect(jsonPath("$.content[1].type").value("TRANSFER"))
                .andExpect(jsonPath("$.content[1].direction").value("DEBIT"))
                .andExpect(jsonPath("$.content[1].counterpartAccountNumber").value(brunoArs.accountNumber()))
                .andExpect(
                        jsonPath("$.content[2].transactionId").value(cashIn.id().toString()))
                .andExpect(jsonPath("$.content[2].type").value("CASH_IN"))
                .andExpect(jsonPath("$.content[2].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[2].amount").value("100.00"))
                .andExpect(jsonPath("$.content[2].counterpartAccountNumber").value(nullValue()))
                .andExpect(jsonPath("$.content[2].id").isNotEmpty())
                .andExpect(jsonPath("$.content[2].createdAt").isNotEmpty());

        mockMvc.perform(get(entriesOf(brunoArs)).header(HttpHeaders.AUTHORIZATION, bearer(bruno)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].counterpartAccountNumber").value(anaArs.accountNumber()));
    }

    @Test
    @DisplayName("L6: the history is paginated")
    void paginates() throws Exception {
        var ana = register();
        var wallet = ars(ana);
        for (var amount : new String[] {"1.00", "2.00", "3.00"}) {
            cashIn(wallet, amount);
        }

        mockMvc.perform(get(entriesOf(wallet) + "?page=0&size=2").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].amount").value("3.00"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get(entriesOf(wallet) + "?page=1&size=2").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value("1.00"));
        mockMvc.perform(get(entriesOf(wallet)).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("L6: page must be at least 0 and size between 1 and 100")
    void validatesPagination() throws Exception {
        var ana = register();
        var wallet = ars(ana);

        mockMvc.perform(get(entriesOf(wallet) + "?size=101").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        mockMvc.perform(get(entriesOf(wallet) + "?size=0").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(entriesOf(wallet) + "?page=-1").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
        mockMvc.perform(get(entriesOf(wallet) + "?page=abc").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("W10: another customer's wallet history answers as if the wallet did not exist")
    void hidesOtherCustomersHistory() throws Exception {
        var ana = register();
        var bruno = register();
        cashIn(ars(bruno), "10.00");

        mockMvc.perform(get(entriesOf(ars(bruno))).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/wallets/" + UUID.randomUUID() + "/entries")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(entriesOf(ars(ana)))).andExpect(status().isUnauthorized());
    }

    private LedgerTransaction cashIn(Wallet wallet, String amount) {
        return move(
                TransactionType.CASH_IN,
                amount,
                systemWallets.id(SystemPurpose.EXTERNAL, wallet.currency()),
                wallet.id());
    }

    private LedgerTransaction move(TransactionType type, String amount, UUID from, UUID to) {
        return tx.execute(status -> ledger.move(type, Money.of(amount, Currency.ARS), null, from, to));
    }

    private Registration register() {
        return registrationService.registerCustomer("history-" + UUID.randomUUID() + "@example.com", "secret-pass");
    }

    private static Wallet ars(Registration registration) {
        return registration.wallets().stream()
                .filter(w -> w.currency() == Currency.ARS)
                .findFirst()
                .orElseThrow();
    }

    private static String entriesOf(Wallet wallet) {
        return "/api/v1/wallets/" + wallet.id() + "/entries";
    }

    private String bearer(Registration registration) {
        return "Bearer "
                + loginService.login(registration.user().email(), "secret-pass").value();
    }
}
