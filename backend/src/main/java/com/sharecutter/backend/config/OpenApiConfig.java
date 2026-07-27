package com.sharecutter.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME =
            "bearerAuth";

    @Bean
    public OpenAPI shareCutterOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title(
                                        "ShareCutter API"
                                )
                                .description(
                                        """
                                        REST API for managing ShareCutter users,
                                        investment portfolios, assets and transactions.
                                        """
                                )
                                .version(
                                        "1.0.0"
                                )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        createBearerSecurityScheme()
                                )
                );
    }

    private SecurityScheme createBearerSecurityScheme() {
        return new SecurityScheme()
                .name(
                        SECURITY_SCHEME_NAME
                )
                .type(
                        SecurityScheme.Type.HTTP
                )
                .scheme(
                        "bearer"
                )
                .bearerFormat(
                        "JWT"
                )
                .description(
                        """
                        Enter the JWT access token.

                        Swagger UI automatically adds the required
                        "Bearer" prefix to authenticated requests.
                        """
                );
    }
}