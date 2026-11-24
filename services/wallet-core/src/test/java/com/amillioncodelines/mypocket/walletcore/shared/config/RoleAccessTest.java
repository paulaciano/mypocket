package com.amillioncodelines.mypocket.walletcore.shared.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class RoleAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @Test
    @DisplayName("S3: /admin endpoints require the ADMIN role")
    void adminEndpointsRequireAdmin() throws Exception {
        // Nothing is mapped under /admin yet: 404 means the request passed authorization.
        mockMvc.perform(get("/api/v1/admin/anything").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/anything").header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("S3: customer endpoints require the CUSTOMER role")
    void customerEndpointsRequireCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/anything").header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/anything").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private String bearer(String role) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(15)))
                .claim("role", role)
                .build();
        var token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return "Bearer " + token.getTokenValue();
    }
}
