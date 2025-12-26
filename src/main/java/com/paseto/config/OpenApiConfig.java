package com.paseto.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "PASETO Authentication";

    @Bean
    public OpenAPI pasetoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PASETO RESTful API")
                        .description("""
                                ## PASETO Authentication API

                                RESTful API with PASETO v4 authentication.

                                ### Authentication Flow
                                1. **Register** - Create a new account at `POST /api/auth/register`
                                2. **Login** - Get access token (15min) and refresh token (7 days) at `POST /api/auth/login`
                                3. **Use Access Token** - Include in Authorization header: `Bearer <access_token>`
                                4. **Refresh Token** - Get new access token at `POST /api/auth/refresh`
                                5. **Logout** - Revoke refresh token at `POST /api/auth/logout`

                                ### Token Types
                                - **Access Token** (v4.local): Short-lived (15 minutes), used for API calls
                                - **Refresh Token** (v4.public): Long-lived (7 days), used to get new access tokens

                                ### Public Endpoints
                                - `/api/auth/login` - User login
                                - `/api/auth/register` - User registration
                                - `/api/auth/refresh` - Refresh access token
                                - `/api/auth/logout` - User logout
                                - `/api/banners/**` - Banner management (no authentication)

                                ### Protected Endpoints
                                - `/api/products/**` - Product management (requires authentication)
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@paseto.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://api.paseto.com")
                                .description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("PASETO")
                                        .description("""
                                                **PASETO Access Token**

                                                Format: `v4.local.<token>`

                                                How to get token:
                                                1. POST /api/auth/register
                                                2. POST /api/auth/login

                                                Token expires in 15 minutes.
                                                """)));
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("all")
                .packagesToScan("com.paseto.controller")
                .addOpenApiCustomizer(responseTimeCustomizer())
                .build();
    }

    private OpenApiCustomizer responseTimeCustomizer() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .forEach(operation -> {
                    io.swagger.v3.oas.models.headers.Header responseTimeHeader =
                            new io.swagger.v3.oas.models.headers.Header()
                                    .description("Request processing time in milliseconds")
                                    .schema(new StringSchema().example("45 ms"));

                    operation.getResponses().forEach((code, response) -> {
                        if (response.getHeaders() == null) {
                            response.setHeaders(new LinkedHashMap<>());
                        }
                        response.getHeaders().put("X-Response-Time", responseTimeHeader);
                    });
                });
    }
}
