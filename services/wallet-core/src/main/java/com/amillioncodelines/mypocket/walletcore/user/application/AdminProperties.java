package com.amillioncodelines.mypocket.walletcore.user.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mypocket.admin")
public record AdminProperties(String email, String password) {

    boolean isConfigured() {
        return hasText(email) || hasText(password);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
