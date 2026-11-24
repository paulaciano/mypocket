package com.amillioncodelines.mypocket.walletcore.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService.Registration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class WalletQueriesTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    LoginService loginService;

    @Test
    @DisplayName("W10: a customer lists their own wallets, ARS first")
    void listsOwnWallets() throws Exception {
        var ana = register();
        register();

        mockMvc.perform(get("/api/v1/wallets").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].currency").value("ARS"))
                .andExpect(jsonPath("$[0].id").value(walletId(ana, 0)))
                .andExpect(jsonPath("$[0].accountNumber")
                        .value(ana.wallets().get(0).accountNumber()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].balance").value("0.00"))
                .andExpect(jsonPath("$[1].currency").value("USD"))
                .andExpect(jsonPath("$[1].id").value(walletId(ana, 1)));
    }

    @Test
    @DisplayName("W10: a customer gets one of their wallets by id")
    void getsOwnWallet() throws Exception {
        var ana = register();

        mockMvc.perform(get("/api/v1/wallets/" + walletId(ana, 1)).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId(ana, 1)))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("W10, S4: another customer's wallet answers exactly like a wallet that does not exist")
    void otherCustomersWalletIsNotFound() throws Exception {
        var ana = register();
        var bob = register();

        var foreign = mockMvc.perform(
                        get("/api/v1/wallets/" + walletId(bob, 0)).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replace(walletId(bob, 0), "{id}");
        var missing = UUID.randomUUID().toString();
        var nonexistent = mockMvc.perform(
                        get("/api/v1/wallets/" + missing).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replace(missing, "{id}");

        assertThat(foreign).isEqualTo(nonexistent);
    }

    @Test
    @DisplayName("W10: a malformed wallet id is a bad request")
    void malformedWalletId() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/not-a-uuid").header(HttpHeaders.AUTHORIZATION, bearer(register())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("walletId"));
    }

    @Test
    @DisplayName("P1: /me returns the caller's own id, email and role, and nothing secret")
    void me() throws Exception {
        var ana = register();

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ana.user().id().toString()))
                .andExpect(jsonPath("$.email").value(ana.user().email()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2"))));
    }

    @Test
    @DisplayName("S1: wallet queries require authentication")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/wallets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    private Registration register() {
        return registrationService.registerCustomer("user-" + UUID.randomUUID() + "@example.com", "secret-pass");
    }

    private String bearer(Registration registration) {
        return "Bearer "
                + loginService.login(registration.user().email(), "secret-pass").value();
    }

    private static String walletId(Registration registration, int index) {
        return registration.wallets().get(index).id().toString();
    }
}
