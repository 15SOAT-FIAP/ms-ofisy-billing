package br.com.ofisy.ms_ofisy_billing.application.payment.exceptions;

import java.util.UUID;

public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(UUID serviceOrderId, Throwable cause) {
        super("Não foi possível criar a cobrança da OS " + serviceOrderId + " no provedor de pagamento", cause);
    }

    private PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PaymentUnavailableException forOrderQuery(String externalId, Throwable cause) {
        return new PaymentUnavailableException(
                "Não foi possível consultar a order " + externalId + " no provedor de pagamento", cause);
    }
}