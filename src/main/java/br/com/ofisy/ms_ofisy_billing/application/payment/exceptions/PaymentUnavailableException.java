package br.com.ofisy.ms_ofisy_billing.application.payment.exceptions;

import java.util.UUID;

public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(UUID serviceOrderId, Throwable cause) {
        super("Não foi possível criar a cobrança da OS " + serviceOrderId + " no provedor de pagamento", cause);
    }
}
