package com.amillioncodelines.mypocket.walletcore.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    static final String ROLE_CLAIM = "role";

    private static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/register"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/auth/login"),
            // Authenticated by the provider signature instead of a JWT (S5).
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/webhooks/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/actuator/health/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/error"),
            // Only served when the development profile enables them (S13).
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/v3/api-docs/**"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/swagger-ui.html"),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/swagger-ui/**"));

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var problems = new ProblemSecurityHandler();
        return http
                // Stateless JSON API without cookies: CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS)
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/**")
                        .hasRole("CUSTOMER")
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(ignoringPublicEndpoints())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .build();
    }

    /**
     * Public endpoints ignore any bearer token, so a client that still sends an expired token can log
     * in again (S1).
     */
    private static BearerTokenResolver ignoringPublicEndpoints() {
        var resolver = new DefaultBearerTokenResolver();
        return request -> PUBLIC_ENDPOINTS.matches(request) ? null : resolver.resolve(request);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
