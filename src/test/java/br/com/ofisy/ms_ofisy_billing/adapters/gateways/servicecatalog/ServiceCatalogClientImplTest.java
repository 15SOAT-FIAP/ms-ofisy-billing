package br.com.ofisy.ms_ofisy_billing.adapters.gateways.servicecatalog;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient.ServiceCatalogView;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServiceCatalogClientImplTest {

    private static final String BASE_URL = "http://core.test";

    private MockRestServiceServer server;
    private ServiceCatalogClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ServiceCatalogClientImpl(builder.build());
    }

    @Test
    void shouldReturnTheNameAndPriceFromTheCore() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/services-catalog/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id": "%s", "name": "Troca de óleo", "description": "Completa", "price": 120.50}
                        """.formatted(id), MediaType.APPLICATION_JSON));

        Optional<ServiceCatalogView> result = client.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(id);
        assertThat(result.get().name()).isEqualTo("Troca de óleo");
        assertThat(result.get().price()).isEqualByComparingTo("120.50");
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheCoreAnswers404() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/services-catalog/" + id))
                .andRespond(withResourceNotFound());

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheCoreAnswersWithoutBody() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/services-catalog/" + id))
                .andRespond(withNoContent());

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheCoreFails() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/services-catalog/" + id))
                .andRespond(withServerError());

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenTheCoreRejectsTheToken() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo(BASE_URL + "/api/v1/services-catalog/" + id))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(client.findById(id)).isEmpty();
        server.verify();
    }
}
