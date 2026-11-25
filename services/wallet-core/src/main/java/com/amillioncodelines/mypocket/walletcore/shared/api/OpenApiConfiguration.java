package com.amillioncodelines.mypocket.walletcore.shared.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "MyPocket wallet-core API", version = "v1"),
        security = @SecurityRequirement(name = OpenApiConfiguration.BEARER))
@SecurityScheme(
        name = OpenApiConfiguration.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
class OpenApiConfiguration {

    static final String BEARER = "bearer";
}
