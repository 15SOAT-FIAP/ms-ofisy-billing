package br.com.ofisy.ms_ofisy_billing.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    private final OpenAPI openAPI = new SwaggerConfig().openAPI();

    @Test
    void shouldDescribeTheBillingApi() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Ofisy Billing API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldDeclareTheJwtBearerSchemeAsTheDefaultSecurity() {
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("BearerToken");

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey("BearerToken"));
    }
}
