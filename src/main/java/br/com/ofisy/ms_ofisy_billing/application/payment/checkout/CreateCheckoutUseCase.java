package br.com.ofisy.ms_ofisy_billing.application.payment.checkout;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateCheckoutUseCase {

    Payment execute(CreateCheckoutCommand cmd);

    record CreateCheckoutCommand(UUID serviceOrderId, UUID quoteId, BigDecimal amount, String idempotencyKey) {}
}
