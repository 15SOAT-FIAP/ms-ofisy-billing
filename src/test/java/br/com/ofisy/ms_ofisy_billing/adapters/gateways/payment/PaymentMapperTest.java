package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    @Test
    void shouldRoundTripBetweenDomainAndEntity() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("42.50"), "ext-1", "https://checkout");

        PaymentEntity entity = PaymentMapper.toEntity(payment);
        assertThat(entity.getExternalId()).isEqualTo("ext-1");
        assertThat(entity.getAmount()).isEqualByComparingTo("42.50");

        Payment rebuilt = PaymentMapper.toDomain(entity);
        assertThat(rebuilt.getServiceOrderId()).isEqualTo(payment.getServiceOrderId());
        assertThat(rebuilt.getStatus()).isEqualTo(payment.getStatus());
        assertThat(rebuilt.getCheckoutUrl()).isEqualTo("https://checkout");
    }
}
