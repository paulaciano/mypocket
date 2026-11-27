package com.amillioncodelines.mypocket.walletcore.cashin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class WebhookSignaturePathTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    WalletRepository wallets;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/v1/webhooks/cash-ins",
                "/api/v1/%77ebhooks/cash-ins",
                "/api/%761/webhooks/cash-ins",
                "/%61pi/v1/webhooks/cash-ins",
                "/api/v1/webhooks/cash%2Dins",
                "/api/v1/webhooks/%63ash-ins"
            })
    @DisplayName("S5: no spelling of a webhook path reaches the webhook without a valid signature")
    void encodedPathsStillNeedSignature(String path) throws Exception {
        var wallet = registrationService
                .registerCustomer("path-" + UUID.randomUUID() + "@example.com", "secret-pass")
                .wallets()
                .getFirst();
        var body = """
                {"reference": "PRV-%s", "accountNumber": "%s", "currency": "%s", "amount": "1000.00"}
                """.formatted(UUID.randomUUID(), wallet.accountNumber(), wallet.currency());

        var status = mockMvc.perform(post(URI.create(path))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(400, 401, 403, 404);
        assertThat(wallets.findById(wallet.id()).orElseThrow().balance()).isEqualByComparingTo("0.00");
    }
}
