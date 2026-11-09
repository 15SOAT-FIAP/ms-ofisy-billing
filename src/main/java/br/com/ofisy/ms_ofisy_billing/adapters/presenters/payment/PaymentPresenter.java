package br.com.ofisy.ms_ofisy_billing.adapters.presenters.payment;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment.dto.PaymentResponseDTO;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentPresenter {

    public static PaymentResponseDTO present(Payment payment) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getServiceOrderId(),
                payment.getQuoteId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCheckoutUrl(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
