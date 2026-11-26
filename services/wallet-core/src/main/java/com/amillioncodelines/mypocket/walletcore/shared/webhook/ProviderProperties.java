package com.amillioncodelines.mypocket.walletcore.shared.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mypocket.provider")
public record ProviderProperties(String webhookSecret) {}
