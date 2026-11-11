package br.com.ofisy.ms_ofisy_billing.application.payment.sync;

import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase.CreateCheckoutCommand;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderStatusResponse;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SyncPaymentService implements SyncPaymentUseCase {

    private static final String PAYMENT_CONFIRMED = "PaymentConfirmed";
    private static final String PAYMENT_FAILED = "PaymentFailed";

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentClient paymentClient;
    private final CreateCheckoutUseCase createCheckoutUseCase;

    @Override
    public void execute(String externalId) {
        Optional<Payment> found = paymentRepository.findByExternalIdForUpdate(externalId);
        if (found.isEmpty()) {
            log.warn("SyncPaymentService.execute >> no payment for order {}, ignoring notification", externalId);
            return;
        }

        Payment payment = found.get();
        PaymentOrderStatusResponse order = paymentClient.getOrder(externalId);

        boolean changed = switch (order.status()) {
            case PAID -> confirm(payment);
            case FAILED -> failAndOpenNewCheckout(payment, order.statusDetail());
            case EXPIRED -> expire(payment, order.statusDetail());
            case PENDING -> false;
        };
        if (changed) {
            return;
        }

        if (PaymentStatus.PENDING.equals(order.status())) {
            log.info("SyncPaymentService.execute >> order {} without actionable status ({})",
                    externalId, order.statusDetail());
        } else if (!order.status().equals(payment.getStatus())) {
            log.warn("SyncPaymentService.execute >> order {} is {} but payment {} is already {}, ignoring",
                    externalId, order.status(), payment.getId(), payment.getStatus());
        }
    }

    private boolean confirm(Payment payment) {
        if (!payment.markPaid()) {
            return false;
        }
        paymentRepository.save(payment);
        outboxEventRepository.save(paymentConfirmedEvent(payment));
        return true;
    }

    private boolean failAndOpenNewCheckout(Payment payment, String statusDetail) {
        if (!payment.markFailed(statusDetail)) {
            return false;
        }
        paymentRepository.save(payment);
        outboxEventRepository.save(paymentFailedEvent(payment));
        createCheckoutUseCase.execute(new CreateCheckoutCommand(payment.getServiceOrderId(),
                payment.getQuoteId(), payment.getAmount(), payment.getId().toString()));
        return true;
    }

    private boolean expire(Payment payment, String statusDetail) {
        if (!payment.markExpired(statusDetail)) {
            return false;
        }
        paymentRepository.save(payment);
        outboxEventRepository.save(paymentFailedEvent(payment));
        return true;
    }

    private OutboxEvent paymentConfirmedEvent(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("amount", payment.getAmount());
        return OutboxEvent.create(PAYMENT_CONFIRMED, payment.getServiceOrderId(), payload);
    }

    private OutboxEvent paymentFailedEvent(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("reason", payment.getStatus().name());
        payload.put("statusDetail", payment.getStatusDetail());
        return OutboxEvent.create(PAYMENT_FAILED, payment.getServiceOrderId(), payload);
    }
}