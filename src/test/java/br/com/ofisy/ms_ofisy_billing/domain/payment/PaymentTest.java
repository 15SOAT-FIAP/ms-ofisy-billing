package br.com.ofisy.ms_ofisy_billing.domain.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.exceptions.InvalidPaymentStatusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void shouldCreateWithPendingStatus() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("300.00"),
                "ofisy-os-abc", "https://mp.example.com/checkout");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExternalId()).isEqualTo("ofisy-os-abc");
    }

    @Test
    void shouldConfirmPendingPayment() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ext", null);

        payment.confirm();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void shouldNotConfirmAnAlreadyConfirmedPayment() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ext", null);
        payment.confirm();

        assertThatThrownBy(payment::confirm).isInstanceOf(InvalidPaymentStatusException.class);
    }

    @Test
    void shouldRejectPendingPayment() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ext", null);

        payment.reject();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void shouldNotRejectAConfirmedPayment() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "ext", null);
        payment.confirm();

        assertThatThrownBy(payment::reject).isInstanceOf(InvalidPaymentStatusException.class);
    }
}
