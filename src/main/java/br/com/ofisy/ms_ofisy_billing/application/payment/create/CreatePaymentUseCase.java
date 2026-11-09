package br.com.ofisy.ms_ofisy_billing.application.payment.create;

import java.util.UUID;

public interface CreatePaymentUseCase {

    void execute(CreatePaymentCommand cmd);

    record CreatePaymentCommand(UUID eventId, String eventType, UUID serviceOrderId) {}
}
