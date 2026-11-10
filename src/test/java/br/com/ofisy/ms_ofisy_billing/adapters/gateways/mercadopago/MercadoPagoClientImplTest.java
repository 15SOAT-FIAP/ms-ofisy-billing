package br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago;

import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderRequest;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderResponse;
import br.com.ofisy.ms_ofisy_billing.config.MercadoPagoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoClientImplTest {

    private static final String BASE_URL = "http://mercadopago.test";
    private static final String ORDER_ID = "ORDTST01KS5AJ6HTK2HRQ3XJ3C2JCKP9";
    private static final String CHECKOUT_URL =
            "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=" + ORDER_ID;

    private MockRestServiceServer server;
    private MercadoPagoClientImpl client;
    private final MercadoPagoProperties properties = new MercadoPagoProperties();

    private final UUID serviceOrderId = UUID.randomUUID();
    private final String idempotencyKey = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        properties.setExpirationTime("P3D");
        client = new MercadoPagoClientImpl(builder.build(), properties);
    }

    @Test
    void shouldCreateTheOrderAndReturnItsIdAndCheckoutUrl() {
        server.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", idempotencyKey))
                .andExpect(content().json("""
                        {"type": "online", "processing_mode": "manual", "total_amount": "250.50",
                         "external_reference": "%s", "expiration_time": "P3D",
                         "items": [{"title": "Ordem de serviço %s", "unit_price": "250.50", "quantity": 1}]}
                        """.formatted(serviceOrderId, serviceOrderId), true))
                .andRespond(withSuccess("""
                        {"id": "%s", "type": "online", "status": "created", "status_detail": "created",
                         "total_amount": "250.50", "checkout_url": "%s"}
                        """.formatted(ORDER_ID, CHECKOUT_URL), MediaType.APPLICATION_JSON));

        PaymentOrderResponse response = client.createOrder(request(new BigDecimal("250.5")));

        assertThat(response.externalId()).isEqualTo(ORDER_ID);
        assertThat(response.checkoutUrl()).isEqualTo(CHECKOUT_URL);
        server.verify();
    }

    @Test
    void shouldAllowRetriesOnTheSameOrderWhenEnabled() {
        properties.setRetriesAllowed(true);
        server.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(jsonPath("$.config.online.retries.allowed").value(true))
                .andRespond(withSuccess("""
                        {"id": "%s", "status": "created", "checkout_url": "%s"}
                        """.formatted(ORDER_ID, CHECKOUT_URL), MediaType.APPLICATION_JSON));

        client.createOrder(request(BigDecimal.TEN));

        server.verify();
    }

    @Test
    void shouldNotSendTheConfigWhenRetriesAreDisabled() {
        properties.setRetriesAllowed(false);
        server.expect(requestTo(BASE_URL + "/v1/orders"))
                .andExpect(jsonPath("$.config").doesNotExist())
                .andRespond(withSuccess("""
                        {"id": "%s", "status": "created", "checkout_url": "%s"}
                        """.formatted(ORDER_ID, CHECKOUT_URL), MediaType.APPLICATION_JSON));

        client.createOrder(request(BigDecimal.TEN));

        server.verify();
    }

    @Test
    void shouldFailWhenTheMercadoPagoIsUnavailable() {
        server.expect(requestTo(BASE_URL + "/v1/orders")).andRespond(withServerError());

        assertThatThrownBy(() -> client.createOrder(request(BigDecimal.TEN)))
                .isInstanceOf(PaymentUnavailableException.class);
        server.verify();
    }

    @Test
    void shouldFailWhenTheMercadoPagoDoesNotAnswer() {
        server.expect(requestTo(BASE_URL + "/v1/orders")).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });

        assertThatThrownBy(() -> client.createOrder(request(BigDecimal.TEN)))
                .isInstanceOf(PaymentUnavailableException.class);
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "UNAUTHORIZED", "CONFLICT"})
    void shouldFailWhenTheMercadoPagoRejectsTheRequest(HttpStatus status) {
        server.expect(requestTo(BASE_URL + "/v1/orders")).andRespond(withStatus(status));

        assertThatThrownBy(() -> client.createOrder(request(BigDecimal.TEN)))
                .isInstanceOf(PaymentUnavailableException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\": \"ORD-1\", \"status\": \"created\"}",
            "{\"status\": \"created\", \"checkout_url\": \"https://checkout\"}"
    })
    void shouldFailWhenTheResponseHasNoIdOrCheckoutUrl(String body) {
        server.expect(requestTo(BASE_URL + "/v1/orders"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createOrder(request(BigDecimal.TEN)))
                .isInstanceOf(PaymentUnavailableException.class);
        server.verify();
    }

    @Test
    void shouldFailWhenTheResponseHasNoBody() {
        server.expect(requestTo(BASE_URL + "/v1/orders")).andRespond(withNoContent());

        assertThatThrownBy(() -> client.createOrder(request(BigDecimal.TEN)))
                .isInstanceOf(PaymentUnavailableException.class);
        server.verify();
    }

    private PaymentOrderRequest request(BigDecimal amount) {
        return new PaymentOrderRequest(idempotencyKey, serviceOrderId, amount);
    }
}
