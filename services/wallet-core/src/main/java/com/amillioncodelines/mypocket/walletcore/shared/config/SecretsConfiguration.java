package com.amillioncodelines.mypocket.walletcore.shared.config;

import java.util.LinkedHashMap;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Validates secrets while the context starts, so a misconfigured instance never serves requests. */
@Configuration(proxyBeanMethods = false)
class SecretsConfiguration {

    SecretsConfiguration(Environment environment) {
        var secrets = new LinkedHashMap<String, String>();
        secrets.put("MYPOCKET_JWT_SECRET", environment.getProperty("mypocket.security.jwt.secret"));
        secrets.put("MYPOCKET_PROVIDER_WEBHOOK_SECRET", environment.getProperty("mypocket.provider.webhook-secret"));
        secrets.put("MYPOCKET_DB_PASSWORD", environment.getProperty("MYPOCKET_DB_PASSWORD"));
        var adminPassword = environment.getProperty("mypocket.admin.password");
        if (adminPassword != null && !adminPassword.isBlank()) {
            secrets.put("MYPOCKET_ADMIN_PASSWORD", adminPassword);
        }
        SecretsValidator.validate(
                secrets,
                Set.of("MYPOCKET_JWT_SECRET", "MYPOCKET_PROVIDER_WEBHOOK_SECRET"),
                Set.of(environment.getActiveProfiles()));
    }
}
