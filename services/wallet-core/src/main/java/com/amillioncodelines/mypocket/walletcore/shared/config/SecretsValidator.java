package com.amillioncodelines.mypocket.walletcore.shared.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Refuses to start with weak or example secrets. Example values (marked with "example") are only
 * accepted in the dev and test profiles.
 */
final class SecretsValidator {

    static final int MIN_KEY_BYTES = 32;
    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("dev", "test");

    private SecretsValidator() {}

    /** {@code keys} are HMAC keys among {@code secrets}: they need at least 32 bytes in every profile. */
    static void validate(Map<String, String> secrets, Set<String> keys, Set<String> activeProfiles) {
        for (var name : keys) {
            var key = secrets.get(name);
            if (key == null || key.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_BYTES) {
                throw new IllegalStateException(name + " must have at least " + MIN_KEY_BYTES + " bytes");
            }
        }
        if (activeProfiles.stream().anyMatch(NON_PRODUCTION_PROFILES::contains)) {
            return;
        }
        secrets.forEach((name, value) -> {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(name + " is required");
            }
            if (value.toLowerCase(Locale.ROOT).contains("example")) {
                throw new IllegalStateException(name + " still has an example value");
            }
        });
    }
}
