package com.amillioncodelines.mypocket.walletcore.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SecretsSourceTest {

    @Test
    @DisplayName("S10: packaged configuration reads secrets from the environment, without default values")
    void secretsComeFromEnvironment() throws IOException {
        var config = new ClassPathResource("application.yaml").getContentAsString(StandardCharsets.UTF_8);

        assertThat(config)
                .contains("${MYPOCKET_DB_PASSWORD}")
                .contains("${MYPOCKET_JWT_SECRET}")
                .contains("${MYPOCKET_ADMIN_PASSWORD:}")
                .doesNotContainPattern("\\$\\{MYPOCKET_(DB_PASSWORD|JWT_SECRET):")
                .doesNotContainIgnoringCase("example");
    }

    @Test
    @DisplayName("S10: the development profile with example values is not packaged")
    void devProfileIsNotPackaged() {
        assertThat(new ClassPathResource("application-dev.yaml").exists()).isFalse();
    }
}
