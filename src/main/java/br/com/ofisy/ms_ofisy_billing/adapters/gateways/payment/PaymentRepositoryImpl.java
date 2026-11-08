package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity saved = jpaPaymentRepository.save(PaymentMapper.toEntity(payment));
        return PaymentMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaPaymentRepository.findById(id).map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByExternalId(String externalId) {
        return jpaPaymentRepository.findByExternalId(externalId).map(PaymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByServiceOrderId(UUID serviceOrderId) {
        return jpaPaymentRepository.findByServiceOrderId(serviceOrderId).stream()
                .map(PaymentMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByServiceOrderIdAndStatus(UUID serviceOrderId, PaymentStatus status) {
        return jpaPaymentRepository.existsByServiceOrderIdAndStatus(serviceOrderId, status);
    }
}
