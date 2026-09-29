package com.digiteen.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI userServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digiteen User Service API")
                        .version("1.0.0")
                        .description("User registration, authentication, refresh-token rotation, "
                                + "and public-key discovery for the Digiteen wallet system.")
                        .contact(new Contact().name("Digiteen Technical Assessment"))
                        .license(new License().name("Private assessment project")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("RS256 access token returned by the login or register endpoint")));
    }
}
