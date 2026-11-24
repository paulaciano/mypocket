package com.amillioncodelines.mypocket.walletcore.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mypocket.security.jwt")
public record JwtProperties(String secret, Duration timeToLive) {}
