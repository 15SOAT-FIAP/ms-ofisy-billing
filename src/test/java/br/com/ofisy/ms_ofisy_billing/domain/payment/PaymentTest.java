package br.com.ofisy.ms_ofisy_billing.domain.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    void shouldCreateWithPendingStatus() {
        Payment payment = pendingPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getExternalId()).isEqualTo("ORD-123");
        assertThat(payment.getCheckoutUrl()).isEqualTo("https://mp.example.com/checkout");
        assertThat(payment.getStatusDetail()).isNull();
    }

    @Test
    void shouldMarkAPendingPaymentAsPaid() {
        Payment payment = pendingPayment();

        assertThat(payment.markPaid()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldMarkAPendingPaymentAsFailedKeepingTheDetail() {
        Payment payment = pendingPayment();

        assertThat(payment.markFailed("rejected_by_issuer")).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getStatusDetail()).isEqualTo("rejected_by_issuer");
    }

    @Test
    void shouldMarkAPendingPaymentAsExpiredKeepingTheDetail() {
        Payment payment = pendingPayment();

        assertThat(payment.markExpired("expired")).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getStatusDetail()).isEqualTo("expired");
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PAID", "FAILED", "EXPIRED"})
    void shouldIgnoreATransitionWhenThePaymentIsNoLongerPending(PaymentStatus status) {
        Payment payment = Payment.reconstruct(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, status, "ORD-123", null, "previous_detail", null, null);

        assertThat(payment.markPaid()).isFalse();
        assertThat(payment.markFailed("rejected_by_issuer")).isFalse();
        assertThat(payment.markExpired("expired")).isFalse();
        assertThat(payment.getStatus()).isEqualTo(status);
        assertThat(payment.getStatusDetail()).isEqualTo("previous_detail");
        assertThat(payment.getUpdatedAt()).isNull();
    }

    @Test
    void shouldBlockANewChargeOnlyWhilePendingOrPaid() {
        Payment pending = pendingPayment();
        Payment paid = pendingPayment();
        paid.markPaid();
        Payment failed = pendingPayment();
        failed.markFailed("rejected_by_issuer");
        Payment expired = pendingPayment();
        expired.markExpired("expired");

        assertThat(pending.isPendingOrPaid()).isTrue();
        assertThat(paid.isPendingOrPaid()).isTrue();
        assertThat(failed.isPendingOrPaid()).isFalse();
        assertThat(expired.isPendingOrPaid()).isFalse();
    }

    private Payment pendingPayment() {
        return Payment.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("300.00"),
                "ORD-123", "https://mp.example.com/checkout");
    }
}
