package com.amillioncodelines.mypocket.walletcore.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Answers security failures (401 and 403) with the same Problem Details shape as the rest of the API. */
class ProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String UNAUTHORIZED = """
            {"title":"Unauthorized","status":401,"detail":"Authentication is required.","code":"UNAUTHORIZED"}""";
    private static final String FORBIDDEN = """
            {"title":"Forbidden","status":403,"detail":"Not allowed for this role.","code":"FORBIDDEN"}""";

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, UNAUTHORIZED);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, FORBIDDEN);
    }

    private static void write(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
