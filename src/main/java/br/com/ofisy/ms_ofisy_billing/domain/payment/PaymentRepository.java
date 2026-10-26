package br.com.ofisy.ms_ofisy_billing.domain.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByExternalId(String externalId);

    List<Payment> findByServiceOrderId(UUID serviceOrderId);

    boolean existsByServiceOrderIdAndStatus(UUID serviceOrderId, PaymentStatus status);
}
