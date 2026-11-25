package com.amillioncodelines.mypocket.walletcore.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Same switches the development profile turns on. */
@IntegrationTest
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class ApiDocumentationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("S13: when enabled, the API description and Swagger UI need no token")
    void servedWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/wallets']").exists())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("S1: login and registration are described as public, the rest require a bearer token")
    void describesSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.security[0].bearer").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty());
    }
}
