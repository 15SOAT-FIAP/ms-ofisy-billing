package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote;

import br.com.ofisy.ms_ofisy_billing.TestcontainersConfiguration;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtTestTokens;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "jwt.secret=" + JwtTestTokens.SECRET
)
class QuoteControllerIT {

    private static final String BASE_URL = "/api/v1/quotes";

    @LocalServerPort
    private int port;

    @Autowired
    private QuoteRepository quoteRepository;

    private UUID quoteId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)),
                List.of());
        quoteId = quoteRepository.save(quote).getId();
    }

    @Nested
    class Authorization {

        @Test
        void shouldReturn401WhenTokenIsMissing() {
            RestAssured.given()
                    .when()
                    .get(BASE_URL + "/{id}", quoteId)
                    .then()
                    .statusCode(401)
                    .body("status", equalTo(401))
                    .body("title", equalTo("Usuário não autorizado"));
        }

        @Test
        void shouldReturn401WhenTokenIsSignedWithAnotherSecret() {
            getById(JwtTestTokens.withRole(JwtTestTokens.OTHER_SECRET, "MECHANIC")).statusCode(401);
        }

        @Test
        void shouldReturn401WhenTokenIsExpired() {
            getById(JwtTestTokens.expired(JwtTestTokens.SECRET, "MECHANIC")).statusCode(401);
        }

        @Test
        void shouldReturn401WhenTokenWasIssuedForACustomer() {
            getById(JwtTestTokens.withIssuer(JwtTestTokens.SECRET, JwtTestTokens.CUSTOMER_ISSUER, "MECHANIC"))
                    .statusCode(401);
        }

        @Test
        void shouldReturn403WhenRoleIsNotAllowed() {
            getById(JwtTestTokens.withRole(JwtTestTokens.SECRET, "STOCKMAN"))
                    .statusCode(403)
                    .body("status", equalTo(403))
                    .body("title", equalTo("Usuário sem permissão"));
        }

        @Test
        void shouldReturn403WhenTokenHasNoRole() {
            getById(JwtTestTokens.withoutRole(JwtTestTokens.SECRET)).statusCode(403);
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMIN", "ATTENDANT", "MECHANIC"})
        void shouldReturn200ForAllowedRoles(String role) {
            getById(JwtTestTokens.withRole(JwtTestTokens.SECRET, role))
                    .statusCode(200)
                    .body("id", equalTo(quoteId.toString()));
        }

        @Test
        void shouldNotRequireATokenToApprove() {
            RestAssured.given()
                    .when()
                    .patch(BASE_URL + "/{id}/approve", UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldNotRequireATokenToReprove() {
            RestAssured.given()
                    .contentType("application/json")
                    .body("{\"reason\": \"Preço acima do esperado\"}")
                    .when()
                    .patch(BASE_URL + "/{id}/reprove", UUID.randomUUID())
                    .then()
                    .statusCode(404);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/actuator/health", "/v3/api-docs", "/swagger-ui/index.html"})
        void shouldAllowPublicPathsWithoutToken(String path) {
            RestAssured.given()
                    .when()
                    .get(path)
                    .then()
                    .statusCode(200);
        }

        private ValidatableResponse getById(String token) {
            return RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(BASE_URL + "/{id}", quoteId)
                    .then();
        }
    }
}
