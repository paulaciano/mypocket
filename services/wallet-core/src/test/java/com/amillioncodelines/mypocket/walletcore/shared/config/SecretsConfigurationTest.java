package com.amillioncodelines.mypocket.walletcore.shared.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecretsConfigurationTest {

    private static final String STRONG = "f3a9c1d27b8e4065a1c3d5e7f9b2c4d6e8";

    @Test
    @DisplayName("S11: outside dev and test, refuses an example administrator password")
    void rejectsExampleAdminPassword() {
        var environment = productionEnvironment().withProperty("mypocket.admin.password", "admin-dev-example");

        assertThatThrownBy(() -> new SecretsConfiguration(environment))
                .hasMessage("MYPOCKET_ADMIN_PASSWORD still has an example value");
    }

    @Test
    @DisplayName("S11, U5: the administrator password is not required when no administrator is configured")
    void adminPasswordIsOptional() {
        assertThatCode(() -> new SecretsConfiguration(productionEnvironment())).doesNotThrowAnyException();
    }

    private static MockEnvironment productionEnvironment() {
        return new MockEnvironment()
                .withProperty("mypocket.security.jwt.secret", STRONG)
                .withProperty("MYPOCKET_DB_PASSWORD", "s3cr3t");
    }
}
