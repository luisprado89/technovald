package com.technovald.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Metadatos de OpenAPI y esquema Bearer para el botón Authorize de Swagger UI. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TechNovaLD API REST",
                version = "1.0",
                description = "API REST de TechNovaLD. Autenticación con JWT."
        ),
        // Aplicamos seguridad global: hace que aparezca el candado en TODOS los endpoints.
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth", // Nombre identificador del esquema de seguridad
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}