package br.com.ofisy.ms_ofisy_billing.domain.payment;

import br.com.ofisy.ms_ofisy_billing.domain.payment.exceptions.InvalidPaymentStatusException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    public static final String ACTION_CONFIRM = "confirmar";
    public static final String ACTION_REJECT = "recusar";

    private UUID id;
    private UUID serviceOrderId;
    private UUID quoteId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String externalId;
    private String checkoutUrl;
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
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        Payment payment = new Payment();
        payment.id = id;
        payment.serviceOrderId = serviceOrderId;
        payment.quoteId = quoteId;
        payment.amount = amount;
        payment.status = status;
        payment.externalId = externalId;
        payment.checkoutUrl = checkoutUrl;
        payment.createdAt = createdAt;
        payment.updatedAt = updatedAt;
        return payment;
    }

    public void confirm() {
        if (!PaymentStatus.PENDING.equals(this.status)) {
            throw new InvalidPaymentStatusException(ACTION_CONFIRM, this.status);
        }
        this.status = PaymentStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        if (!PaymentStatus.PENDING.equals(this.status)) {
            throw new InvalidPaymentStatusException(ACTION_REJECT, this.status);
        }
        this.status = PaymentStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }
}
