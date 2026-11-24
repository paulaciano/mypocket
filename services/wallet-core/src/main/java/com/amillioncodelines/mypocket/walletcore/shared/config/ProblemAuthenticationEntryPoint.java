package com.amillioncodelines.mypocket.walletcore.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Answers unauthenticated requests with the same Problem Details shape as the rest of the API. */
class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BODY = """
            {"title":"Unauthorized","status":401,"detail":"Authentication is required.","code":"UNAUTHORIZED"}""";

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(BODY);
    }
}
