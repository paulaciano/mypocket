package com.amillioncodelines.mypocket.walletcore.shared.config;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
class JwtConfiguration {

    private final SecretKey key;

    JwtConfiguration(JwtProperties properties, Environment environment) {
        var secrets = new LinkedHashMap<String, String>();
        secrets.put("MYPOCKET_JWT_SECRET", properties.secret());
        secrets.put("MYPOCKET_DB_PASSWORD", environment.getProperty("MYPOCKET_DB_PASSWORD"));
        SecretsValidator.validate(secrets, properties.secret(), Set.of(environment.getActiveProfiles()));
        this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
