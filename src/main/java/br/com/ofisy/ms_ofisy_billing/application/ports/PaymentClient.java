package br.com.ofisy.ms_ofisy_billing.application.ports;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {

    PaymentOrderResponse createOrder(PaymentOrderRequest request);

    record PaymentOrderRequest(String idempotencyKey, UUID serviceOrderId, BigDecimal amount) {}

    record PaymentOrderResponse(String externalId, String checkoutUrl) {}
}
