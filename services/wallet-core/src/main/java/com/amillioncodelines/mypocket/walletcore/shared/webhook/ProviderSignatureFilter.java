package com.amillioncodelines.mypocket.walletcore.shared.webhook;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies the provider signature over the raw body before anything parses it (S5, ADR-0008). Only
 * requests with a valid signature reach the webhook controllers.
 */
@Component
// The secret is validated (S11) before the filter reads it.
@DependsOn("secretsConfiguration")
@EnableConfigurationProperties(ProviderProperties.class)
class ProviderSignatureFilter extends OncePerRequestFilter {

    static final String WEBHOOKS_PATH = "/api/v1/webhooks/";
    static final String TIMESTAMP_HEADER = "X-Provider-Timestamp";
    static final String SIGNATURE_HEADER = "X-Provider-Signature";
    static final int MAX_BODY_BYTES = 16 * 1024;

    private static final Logger log = LoggerFactory.getLogger(ProviderSignatureFilter.class);
    private static final String INVALID_SIGNATURE = """
            {"title":"Invalid signature","status":401,"detail":"The webhook signature is missing, invalid or expired.","code":"INVALID_SIGNATURE"}""";
    private static final String TOO_LARGE = """
            {"title":"Payload too large","status":413,"detail":"The webhook body is too large.","code":"PAYLOAD_TOO_LARGE"}""";

    private final byte[] secret;
    private final Clock clock;

    ProviderSignatureFilter(ProviderProperties properties, Clock clock) {
        this.secret = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(WEBHOOKS_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            write(response, HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE);
            return;
        }
        var valid = ProviderSignature.isValid(
                secret,
                request.getHeader(TIMESTAMP_HEADER),
                request.getHeader(SIGNATURE_HEADER),
                body,
                clock.instant());
        if (!valid) {
            log.warn("Provider webhook rejected: invalid signature on {}", request.getRequestURI());
            write(response, HttpStatus.UNAUTHORIZED, INVALID_SIGNATURE);
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private static void write(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
