package br.com.ofisy.ms_ofisy_billing.adapters.gateways.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentMapper {

    public static PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId())
                .serviceOrderId(payment.getServiceOrderId())
                .quoteId(payment.getQuoteId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .externalId(payment.getExternalId())
                .checkoutUrl(payment.getCheckoutUrl())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public static Payment toDomain(PaymentEntity entity) {
        return Payment.reconstruct(
                entity.getId(),
                entity.getServiceOrderId(),
                entity.getQuoteId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getExternalId(),
                entity.getCheckoutUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
