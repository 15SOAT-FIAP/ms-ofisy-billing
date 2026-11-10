package br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago;

import br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago.dto.CreateOrderRequest;
import br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago.dto.OrderResponse;
import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoClientImpl implements PaymentClient {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String ORDER_TYPE = "online";
    private static final String PROCESSING_MODE = "manual";
    private static final String ITEM_TITLE = "Ordem de serviço ";

    private final RestClient mercadoPagoRestClient;
    private final MercadoPagoProperties properties;

    @Override
    public PaymentOrderResponse createOrder(PaymentOrderRequest request) {
        String amount = request.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        CreateOrderRequest body = new CreateOrderRequest(
                ORDER_TYPE,
                PROCESSING_MODE,
                amount,
                request.serviceOrderId().toString(),
                properties.getExpirationTime(),
                List.of(new CreateOrderRequest.Item(ITEM_TITLE + request.serviceOrderId(), amount, 1)),
                retriesConfig());

        OrderResponse response;
        try {
            response = mercadoPagoRestClient.post()
                    .uri("/v1/orders")
                    .header(IDEMPOTENCY_KEY_HEADER, request.idempotencyKey())
                    .body(body)
                    .retrieve()
                    .body(OrderResponse.class);
        } catch (Exception e) {
            log.error("Falha ao criar a order da OS {} no Mercado Pago", request.serviceOrderId(), e);
            throw new PaymentUnavailableException(request.serviceOrderId(), e);
        }

        if (response == null || response.id() == null || response.checkoutUrl() == null) {
            throw new PaymentUnavailableException(request.serviceOrderId(),
                    new IllegalStateException("Resposta do Mercado Pago sem id ou checkout_url"));
        }
        return new PaymentOrderResponse(response.id(), response.checkoutUrl());
    }

    private CreateOrderRequest.Config retriesConfig() {
        if (!properties.isRetriesAllowed()) {
            return null;
        }
        return new CreateOrderRequest.Config(new CreateOrderRequest.Online(new CreateOrderRequest.Retries(true)));
    }
}
