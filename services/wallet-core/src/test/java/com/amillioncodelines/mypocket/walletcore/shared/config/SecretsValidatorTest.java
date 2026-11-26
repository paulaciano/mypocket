package com.amillioncodelines.mypocket.walletcore.shared.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretsValidatorTest {

    private static final String STRONG = "f3a9c1d27b8e4065a1c3d5e7f9b2c4d6e8";

    private static final Set<String> KEYS = Set.of("MYPOCKET_JWT_SECRET", "MYPOCKET_PROVIDER_WEBHOOK_SECRET");

    @Test
    @DisplayName("S11: refuses a JWT or webhook secret shorter than 32 bytes in any profile")
    void rejectsShortKeys() {
        assertThatThrownBy(() -> SecretsValidator.validate(
                        Map.of("MYPOCKET_JWT_SECRET", STRONG, "MYPOCKET_PROVIDER_WEBHOOK_SECRET", "too-short"),
                        KEYS,
                        Set.of("dev")))
                .hasMessage("MYPOCKET_PROVIDER_WEBHOOK_SECRET must have at least 32 bytes");
        assertThatThrownBy(() ->
                        SecretsValidator.validate(Map.of("MYPOCKET_PROVIDER_WEBHOOK_SECRET", STRONG), KEYS, Set.of()))
                .hasMessage("MYPOCKET_JWT_SECRET must have at least 32 bytes");
    }

    @Test
    @DisplayName("S11: outside dev and test, refuses missing secrets and example values")
    void rejectsExampleOrMissingSecretsOutsideDev() {
        assertThatThrownBy(() -> SecretsValidator.validate(
                        Map.of("MYPOCKET_DB_PASSWORD", "mypocket-dev-example"), Set.of(), Set.of()))
                .hasMessage("MYPOCKET_DB_PASSWORD still has an example value");

        var missing = new HashMap<String, String>();
        missing.put("MYPOCKET_DB_PASSWORD", null);
        assertThatThrownBy(() -> SecretsValidator.validate(missing, Set.of(), Set.of("prod")))
                .hasMessage("MYPOCKET_DB_PASSWORD is required");

        var exampleJwt = "an-example-jwt-secret-of-at-least-32-bytes";
        assertThatThrownBy(() -> SecretsValidator.validate(
                        Map.of("MYPOCKET_JWT_SECRET", exampleJwt), Set.of("MYPOCKET_JWT_SECRET"), Set.of()))
                .hasMessage("MYPOCKET_JWT_SECRET still has an example value");
    }

    @Test
    @DisplayName("S11: accepts example values in the dev and test profiles, and real values anywhere")
    void acceptsExampleValuesInDevAndTest() {
        var example = Map.of("MYPOCKET_DB_PASSWORD", "mypocket-dev-example");
        assertThatCode(() -> SecretsValidator.validate(example, Set.of(), Set.of("dev")))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecretsValidator.validate(example, Set.of(), Set.of("test")))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecretsValidator.validate(Map.of("MYPOCKET_DB_PASSWORD", "s3cr3t"), Set.of(), Set.of()))
                .doesNotThrowAnyException();
    }
}
