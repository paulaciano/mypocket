package com.amillioncodelines.mypocket.walletcore.shared.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** S5: {@code sha256=} + hex HMAC-SHA256 of {@code timestamp + "." + raw body}, within 5 minutes. */
final class ProviderSignature {

    static final Duration TOLERANCE = Duration.ofMinutes(5);
    private static final String PREFIX = "sha256=";

    private ProviderSignature() {}

    static boolean isValid(byte[] secret, String timestamp, String signature, byte[] body, Instant now) {
        if (timestamp == null || signature == null || !timestamp.matches("[0-9]{1,12}")) {
            return false;
        }
        var sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
        if (Duration.between(sentAt, now).abs().compareTo(TOLERANCE) > 0) {
            return false;
        }
        var expected = PREFIX + hmac(secret, timestamp, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private static String hmac(byte[] secret, String timestamp, byte[] body) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
