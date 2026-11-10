package br.com.ofisy.ms_ofisy_billing.application.payment.create;

import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase.CreateCheckoutCommand;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CreatePaymentService implements CreatePaymentUseCase {

    private static final String ACTION_CHARGE = "cobrar";

    private final QuoteRepository quoteRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final CreateCheckoutUseCase createCheckoutUseCase;

    @Override
    public void execute(CreatePaymentCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        Quote quote = quoteRepository.findByServiceOrderId(cmd.serviceOrderId()).stream()
                .findFirst()
                .orElseThrow(() -> QuoteNotFoundException.forServiceOrder(cmd.serviceOrderId()));
        if (!quote.isApproved()) {
            throw new InvalidQuoteStatusException(ACTION_CHARGE, quote.getStatus());
        }

        boolean alreadyCharged = paymentRepository.findByServiceOrderId(cmd.serviceOrderId()).stream()
                .anyMatch(Payment::isPendingOrPaid);
        if (alreadyCharged) {
            log.info("CreatePaymentService.execute >> service order {} already has a payment, ignoring event {}",
                    cmd.serviceOrderId(), cmd.eventId());
        } else {
            createCheckoutUseCase.execute(new CreateCheckoutCommand(
                    cmd.serviceOrderId(), quote.getId(), quote.getTotalPrice(), cmd.eventId().toString()));
        }

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }
}
