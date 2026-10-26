package br.com.ofisy.ms_ofisy_billing.application.payment.findbyserviceorderid;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;

import java.util.List;
import java.util.UUID;

public interface FindPaymentByServiceOrderIdUseCase {

    List<Payment> execute(UUID serviceOrderId);
}
