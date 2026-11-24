package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class LoginTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    JwtEncoder jwtEncoder;

    @Test
    @DisplayName("S1, S18: login returns a 15-minute bearer token whose subject is the user id and has the role")
    void issuesToken() throws Exception {
        var email = uniqueEmail();
        var userId = registrationService
                .registerCustomer(email, "secret-pass")
                .user()
                .id();

        var body = login(email.toUpperCase(), "secret-pass")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var jwt = jwtDecoder.decode(JsonPath.read(body, "$.accessToken"));
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getClaims().values()).noneMatch(value -> value.toString().contains(email));
    }

    @Test
    @DisplayName("U4: wrong password and unknown email give the same error")
    void sameErrorForWrongPasswordAndUnknownEmail() throws Exception {
        var email = uniqueEmail();
        registrationService.registerCustomer(email, "secret-pass");

        var wrongPassword = login(email, "wrong-pass")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var unknownEmail = login(uniqueEmail(), "secret-pass")
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    @DisplayName("U4: a password over 72 bytes is just wrong credentials")
    void overlongPasswordIsInvalidCredentials() throws Exception {
        var email = uniqueEmail();
        registrationService.registerCustomer(email, "secret-pass");

        login(email, "ñ".repeat(40))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("S1: a valid token authenticates requests")
    void acceptsValidToken() throws Exception {
        var token = tokenFor(UUID.randomUUID(), Instant.now(), jwtEncoder);

        // No endpoint is mapped here yet: 404 instead of 401 shows the request got past authentication.
        mockMvc.perform(get("/api/v1/unmapped").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("S1: expired, foreign-signed and malformed tokens are rejected")
    void rejectsInvalidTokens() throws Exception {
        var expired = tokenFor(UUID.randomUUID(), Instant.now().minus(Duration.ofHours(1)), jwtEncoder);
        var foreignKey = new SecretKeySpec(
                "another-secret-of-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var foreign = tokenFor(
                UUID.randomUUID(),
                Instant.now(),
                NimbusJwtEncoder.withSecretKey(foreignKey)
                        .algorithm(MacAlgorithm.HS256)
                        .build());

        for (var token : new String[] {expired, foreign, "not-a-jwt"}) {
            mockMvc.perform(get("/api/v1/unmapped").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    private static String tokenFor(UUID userId, Instant issuedAt, JwtEncoder encoder) {
        var claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .claim("role", "CUSTOMER")
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
