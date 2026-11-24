package com.amillioncodelines.mypocket.walletcore.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class PublicEndpointsTest {

    private static final String STALE_TOKEN = "Bearer not-a-valid-or-expired-token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Test
    @DisplayName("S1: a client that still sends an old token can log in again")
    void loginIgnoresStaleToken() throws Exception {
        var email = "user-" + UUID.randomUUID() + "@example.com";
        registrationService.registerCustomer(email, "secret-pass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, STALE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "secret-pass"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("S1: registration and health ignore any bearer token")
    void registrationAndHealthIgnoreStaleToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, STALE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user-%s@example.com", "password": "secret-pass"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, STALE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("S1: protected endpoints still reject the same token")
    void protectedEndpointsStillRejectIt() throws Exception {
        mockMvc.perform(get("/api/v1/wallets").header(HttpHeaders.AUTHORIZATION, STALE_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
