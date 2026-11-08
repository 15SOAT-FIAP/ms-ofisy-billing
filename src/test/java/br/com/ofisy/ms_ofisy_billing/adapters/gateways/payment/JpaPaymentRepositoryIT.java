package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import br.com.ofisy.ms_ofisy_billing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JpaPaymentRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private JpaPaymentRepository jpaPaymentRepository;

    @Test
    void shouldPersistAndFindPaymentByExternalId() {
        UUID serviceOrderId = UUID.randomUUID();

        PaymentEntity entity = PaymentEntity.builder()
                .serviceOrderId(serviceOrderId)
                .quoteId(UUID.randomUUID())
                .amount(new BigDecimal("199.90"))
                .status(PaymentStatus.PENDING)
                .externalId("ofisy-os-" + serviceOrderId)
                .checkoutUrl("https://mp.example.com/checkout")
                .createdAt(LocalDateTime.now())
                .build();

        jpaPaymentRepository.save(entity);

        assertThat(jpaPaymentRepository.findByExternalId("ofisy-os-" + serviceOrderId)).isPresent();
        assertThat(jpaPaymentRepository.existsByServiceOrderIdAndStatus(serviceOrderId, PaymentStatus.PENDING)).isTrue();

        List<PaymentEntity> byServiceOrder = jpaPaymentRepository.findByServiceOrderId(serviceOrderId);
        assertThat(byServiceOrder).hasSize(1);
    }

    @Test
    void shouldEnforceUniqueExternalId() {
        String externalId = "ofisy-os-duplicate";
        jpaPaymentRepository.save(PaymentEntity.builder()
                .serviceOrderId(UUID.randomUUID())
                .quoteId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .externalId(externalId)
                .createdAt(LocalDateTime.now())
                .build());
        jpaPaymentRepository.flush();

        PaymentEntity duplicate = PaymentEntity.builder()
                .serviceOrderId(UUID.randomUUID())
                .quoteId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .externalId(externalId)
                .createdAt(LocalDateTime.now())
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jpaPaymentRepository.save(duplicate);
            jpaPaymentRepository.flush();
        });
    }
}
