package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByExternalId(String externalId);

    List<PaymentEntity> findByServiceOrderId(UUID serviceOrderId);

    boolean existsByServiceOrderIdAndStatus(UUID serviceOrderId, PaymentStatus status);
}
