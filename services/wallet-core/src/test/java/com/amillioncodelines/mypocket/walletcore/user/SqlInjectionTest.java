package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class SqlInjectionTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository users;

    @Test
    @DisplayName("S8: SQL in the login email is treated as data")
    void loginEmailIsData() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "' OR '1'='1", "password": "' OR '1'='1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("S8: quotes in a registered email are stored literally")
    void registeredEmailIsData() throws Exception {
        var email = "o'brien-" + UUID.randomUUID() + "'--@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "secret-pass"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        assertThat(users.findByEmail(email)).isPresent();
    }
}
