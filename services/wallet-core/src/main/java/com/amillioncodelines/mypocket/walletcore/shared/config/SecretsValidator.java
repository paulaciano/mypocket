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

    static final int MIN_JWT_SECRET_BYTES = 32;
    private static final Set<String> NON_PRODUCTION_PROFILES = Set.of("dev", "test");

    private SecretsValidator() {}

    static void validate(Map<String, String> secrets, String jwtSecret, Set<String> activeProfiles) {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException(
                    "MYPOCKET_JWT_SECRET must have at least " + MIN_JWT_SECRET_BYTES + " bytes");
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
