package br.com.ofisy.ms_ofisy_billing.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String BEARER = "bearer";
    public static final String JWT = "JWT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ofisy Billing API")
                        .description("Microsserviço de orçamentos e cobranças da Ofisy")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("15SOAT - Grupo 87")))
                .addSecurityItem(new SecurityRequirement().addList("BearerToken"))
                .components(new Components()
                        .addSecuritySchemes("BearerToken", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(BEARER)
                                .bearerFormat(JWT)));
    }
}
