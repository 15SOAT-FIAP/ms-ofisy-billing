package br.com.ofisy.ms_ofisy_billing.domain.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    private UUID id;
    private UUID serviceOrderId;
    private UUID quoteId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String externalId;
    private String checkoutUrl;
    private String statusDetail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Payment create(UUID serviceOrderId, UUID quoteId, BigDecimal amount,
                                  String externalId, String checkoutUrl) {
        Payment payment = new Payment();
        payment.serviceOrderId = serviceOrderId;
        payment.quoteId = quoteId;
        payment.amount = amount;
        payment.status = PaymentStatus.PENDING;
        payment.externalId = externalId;
        payment.checkoutUrl = checkoutUrl;
        payment.createdAt = LocalDateTime.now();
        payment.updatedAt = LocalDateTime.now();
        return payment;
    }

    public static Payment reconstruct(UUID id, UUID serviceOrderId, UUID quoteId, BigDecimal amount,
                                       PaymentStatus status, String externalId, String checkoutUrl,
                                       String statusDetail, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.serviceOrderId = serviceOrderId;
        payment.quoteId = quoteId;
        payment.amount = amount;
        payment.status = status;
        payment.externalId = externalId;
        payment.checkoutUrl = checkoutUrl;
        payment.statusDetail = statusDetail;
        payment.createdAt = createdAt;
        payment.updatedAt = updatedAt;
        return payment;
    }

    public boolean isPendingOrPaid() {
        return PaymentStatus.PENDING.equals(status) || PaymentStatus.PAID.equals(status);
    }

    public boolean markPaid() {
        return transitionTo(PaymentStatus.PAID, null);
    }

    public boolean markFailed(String detail) {
        return transitionTo(PaymentStatus.FAILED, detail);
    }

    public boolean markExpired(String detail) {
        return transitionTo(PaymentStatus.EXPIRED, detail);
    }

    private boolean transitionTo(PaymentStatus target, String detail) {
        if (!PaymentStatus.PENDING.equals(this.status)) {
            return false;
        }
        this.status = target;
        this.statusDetail = detail;
        this.updatedAt = LocalDateTime.now();
        return true;
    }
}
