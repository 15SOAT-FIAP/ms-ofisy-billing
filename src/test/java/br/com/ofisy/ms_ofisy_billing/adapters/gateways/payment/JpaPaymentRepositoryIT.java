package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import br.com.ofisy.ms_ofisy_billing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .status(PaymentStatus.FAILED)
                .externalId("ofisy-os-" + serviceOrderId)
                .checkoutUrl("https://mp.example.com/checkout")
                .statusDetail("rejected_by_issuer")
                .createdAt(LocalDateTime.now())
                .build();

        jpaPaymentRepository.saveAndFlush(entity);

        assertThat(jpaPaymentRepository.findByExternalId("ofisy-os-" + serviceOrderId))
                .hasValueSatisfying(found -> assertThat(found.getStatusDetail()).isEqualTo("rejected_by_issuer"));
        assertThat(jpaPaymentRepository.existsByServiceOrderIdAndStatus(serviceOrderId, PaymentStatus.FAILED)).isTrue();

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

    @Test
    void shouldRequireExternalId() {
        PaymentEntity withoutExternalId = PaymentEntity.builder()
                .serviceOrderId(UUID.randomUUID())
                .quoteId(UUID.randomUUID())
                .amount(BigDecimal.TEN)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> jpaPaymentRepository.saveAndFlush(withoutExternalId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
