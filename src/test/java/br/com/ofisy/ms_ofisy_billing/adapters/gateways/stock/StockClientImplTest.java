package br.com.ofisy.ms_ofisy_billing.adapters.gateways.stock;

import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient.StockView;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StockClientImplTest {

    private static final String BASE_URL = "http://stock.test";

    private MockRestServiceServer server;
    private StockClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StockClientImpl(builder.build());
    }

    @Test
    void shouldReturnTheProductNameAndUnitPriceFromTheStock() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/stocks/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id": "%s", "productName": "Filtro de óleo", "description": "Original", "quantity": 10,
                         "unitPrice": 35.90, "category": "FILTROS", "minThreshold": 2, "isLowStock": false}
                        """.formatted(id), MediaType.APPLICATION_JSON));

        Optional<StockView> result = client.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().productName()).isEqualTo("Filtro de óleo");
        assertThat(result.get().unitPrice()).isEqualByComparingTo("35.90");
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheStockAnswers404() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/stocks/" + id))
                .andRespond(withResourceNotFound());

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheStockAnswersWithoutBody() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/stocks/" + id))
                .andRespond(withNoContent());

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }

    @Test
    void shouldFailWhenTheStockIsUnavailable() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/stocks/" + id))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.findById(id)).isInstanceOf(StockUnavailableException.class);
        server.verify();
    }

    @Test
    void shouldFailWhenTheStockRejectsTheToken() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/stocks/" + id))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.findById(id)).isInstanceOf(StockUnavailableException.class);
        server.verify();
    }
}
