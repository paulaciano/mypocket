package com.amillioncodelines.mypocket.walletcore.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods = false)
class JsonConfiguration {

    /**
     * Text fields only accept JSON strings. Above all, amounts must be strings (ADR-0009): a JSON number
     * would be accepted after the client may already have lost precision.
     */
    @Bean
    JsonMapperBuilderCustomizer stringsOnlyFromJsonStrings() {
        return builder -> builder.withCoercionConfig(
                LogicalType.Textual,
                config -> config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
