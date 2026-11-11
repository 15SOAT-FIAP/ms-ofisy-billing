package br.com.ofisy.ms_ofisy_billing.application.ports;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentClient {

    PaymentOrderResponse createOrder(PaymentOrderRequest request);

    PaymentOrderStatusResponse getOrder(String externalId);

    record PaymentOrderRequest(String idempotencyKey, UUID serviceOrderId, BigDecimal amount) {}

    record PaymentOrderResponse(String externalId, String checkoutUrl) {}

    record PaymentOrderStatusResponse(PaymentStatus status, String statusDetail) {}
}