package com.ga.waslah.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI waslahOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("WASLAH API")
                        .description("""
                                REST API for jobs and study-space (café) bookings.

                                **How to authorize:** call `POST /auth/login`, copy the `token`, click **Authorize** and paste it (without the `Bearer ` prefix).

                                **Roles:** USER, EMPLOYER, CAFE_MANAGER, ADMIN. Endpoints marked with a lock need a valid JWT; role requirements are listed in each endpoint's description.

                                **Errors:** every error uses the same JSON body: `timestamp`, `status`, `error`, `message`, `path`.
                                """)
                        .version("1.0.0")
                )
                // Every endpoint requires the JWT unless it overrides this with @SecurityRequirements.
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}