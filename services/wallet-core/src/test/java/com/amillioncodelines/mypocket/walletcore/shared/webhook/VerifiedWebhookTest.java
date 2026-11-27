package com.amillioncodelines.mypocket.walletcore.shared.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class VerifiedWebhookTest {

    @Test
    @DisplayName("S5: a webhook request the signature filter did not verify is rejected by the controller")
    void rejectsUnverifiedRequests() {
        var request = new MockHttpServletRequest("POST", "/api/v1/webhooks/cash-ins");

        assertThatThrownBy(() -> VerifiedWebhook.require(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("signature");

        VerifiedWebhook.mark(request);
        assertThatCode(() -> VerifiedWebhook.require(request)).doesNotThrowAnyException();
    }
}
