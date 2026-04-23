package com.winsalty.quickstart.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Configuration
public class OpenApiConfig {

    @Bean
    @ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenAPI springAdminOpenApi() {
        String schemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info().title("Spring Admin Starter API").version("0.0.1-SNAPSHOT"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}
