package br.com.ofisy.ms_ofisy_billing.application.payment.checkout;

import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderRequest;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderResponse;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateCheckoutService implements CreateCheckoutUseCase {

    private static final String PAYMENT_CHECKOUT_CREATED = "PaymentCheckoutCreated";

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentClient paymentClient;

    @Override
    public Payment execute(CreateCheckoutCommand cmd) {
        PaymentOrderResponse order = paymentClient.createOrder(
                new PaymentOrderRequest(cmd.idempotencyKey(), cmd.serviceOrderId(), cmd.amount()));
        Payment saved = paymentRepository.save(Payment.create(
                cmd.serviceOrderId(), cmd.quoteId(), cmd.amount(), order.externalId(), order.checkoutUrl()));
        outboxEventRepository.save(paymentCheckoutCreatedEvent(saved));
        return saved;
    }

    private OutboxEvent paymentCheckoutCreatedEvent(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("checkoutUrl", payment.getCheckoutUrl());
        payload.put("amount", payment.getAmount());
        return OutboxEvent.create(PAYMENT_CHECKOUT_CREATED, payment.getServiceOrderId(), payload);
    }
}
