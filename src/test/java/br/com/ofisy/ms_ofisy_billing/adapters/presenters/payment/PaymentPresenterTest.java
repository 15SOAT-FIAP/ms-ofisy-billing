package br.com.ofisy.ms_ofisy_billing.adapters.presenters.payment;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment.dto.PaymentResponseDTO;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPresenterTest {

    @Test
    void shouldPresentPaymentWithoutExposingTheExternalId() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ext-1", "https://checkout");

        PaymentResponseDTO dto = PaymentPresenter.present(payment);

        assertThat(dto.serviceOrderId()).isEqualTo(payment.getServiceOrderId());
        assertThat(dto.checkoutUrl()).isEqualTo("https://checkout");
    }
}
