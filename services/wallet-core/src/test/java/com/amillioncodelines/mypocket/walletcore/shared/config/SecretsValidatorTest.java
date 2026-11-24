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

    @Test
    @DisplayName("S11: refuses a JWT secret shorter than 32 bytes in any profile")
    void rejectsShortJwtSecret() {
        assertThatThrownBy(() -> SecretsValidator.validate(Map.of(), "too-short", Set.of("dev")))
                .hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> SecretsValidator.validate(Map.of(), null, Set.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("S11: outside dev and test, refuses missing secrets and example values")
    void rejectsExampleOrMissingSecretsOutsideDev() {
        assertThatThrownBy(() -> SecretsValidator.validate(
                        Map.of("MYPOCKET_DB_PASSWORD", "mypocket-dev-example"), STRONG, Set.of()))
                .hasMessage("MYPOCKET_DB_PASSWORD still has an example value");

        var missing = new HashMap<String, String>();
        missing.put("MYPOCKET_DB_PASSWORD", null);
        assertThatThrownBy(() -> SecretsValidator.validate(missing, STRONG, Set.of("prod")))
                .hasMessage("MYPOCKET_DB_PASSWORD is required");

        var exampleJwt = "an-example-jwt-secret-of-at-least-32-bytes";
        assertThatThrownBy(() ->
                        SecretsValidator.validate(Map.of("MYPOCKET_JWT_SECRET", exampleJwt), exampleJwt, Set.of()))
                .hasMessage("MYPOCKET_JWT_SECRET still has an example value");
    }

    @Test
    @DisplayName("S11: accepts example values in the dev and test profiles, and real values anywhere")
    void acceptsExampleValuesInDevAndTest() {
        var example = Map.of("MYPOCKET_DB_PASSWORD", "mypocket-dev-example");
        assertThatCode(() -> SecretsValidator.validate(example, STRONG, Set.of("dev")))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecretsValidator.validate(example, STRONG, Set.of("test")))
                .doesNotThrowAnyException();
        assertThatCode(() -> SecretsValidator.validate(Map.of("MYPOCKET_DB_PASSWORD", "s3cr3t"), STRONG, Set.of()))
                .doesNotThrowAnyException();
    }
}
