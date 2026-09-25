package org.salmanekhalili.jobtrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    /**
     * Registers the JWT scheme so Swagger UI offers an "Authorize" button
     * instead of making every call manually.
     */
    @Bean
    OpenAPI jobtrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("jobtrack API")
                        .version("0.0.1")
                        .description("Job application tracker. Every /api/** endpoint except the two auth "
                                + "endpoints needs `Authorization: Bearer <token>`; resources are scoped to "
                                + "the authenticated user.")
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
