package com.amillioncodelines.mypocket.walletcore.cashin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Plays the payment provider: signs webhooks the way ADR-0008 describes, independently of the production code. */
final class ProviderWebhook {

    static final String SECRET = "test-example-provider-webhook-secret-32-bytes";

    private ProviderWebhook() {}

    static MockHttpServletRequestBuilder signed(String path, String body) {
        return signed(path, body, Instant.now(), SECRET);
    }

    static MockHttpServletRequestBuilder signed(String path, String body, Instant sentAt, String secret) {
        var timestamp = String.valueOf(sentAt.getEpochSecond());
        return post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Provider-Timestamp", timestamp)
                .header("X-Provider-Signature", "sha256=" + hmac(secret, timestamp + "." + body))
                .content(body);
    }

    static String hmac(String secret, String message) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
