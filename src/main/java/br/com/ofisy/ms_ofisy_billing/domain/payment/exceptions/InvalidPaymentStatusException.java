package br.com.ofisy.ms_ofisy_billing.domain.payment.exceptions;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;

public class InvalidPaymentStatusException extends RuntimeException {

    public InvalidPaymentStatusException(String action, PaymentStatus currentStatus) {
        super("Não é possível " + action + " um pagamento no status " + currentStatus + ".");
    }
}
