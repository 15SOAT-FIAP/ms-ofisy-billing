package br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SendQuoteForApprovalService implements SendQuoteForApprovalUseCase {

    private static final String QUOTE_SENT_FOR_APPROVAL = "QuoteSentForApproval";

    private final QuoteRepository quoteRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void execute(SendQuoteForApprovalCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        Quote quote = quoteRepository.findByServiceOrderId(cmd.serviceOrderId()).stream()
                .findFirst()
                .orElseThrow(() -> QuoteNotFoundException.forServiceOrder(cmd.serviceOrderId()));

        if (QuoteStatus.PENDING.equals(quote.getStatus())) {
            quote.sendForApproval();
            Quote saved = quoteRepository.save(quote);
            outboxEventRepository.save(quoteSentForApprovalEvent(saved));
        } else {
            log.warn("SendQuoteForApprovalService.execute >> quote {} is {}, ignoring event {}",
                    quote.getId(), quote.getStatus(), cmd.eventId());
        }

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }

    private OutboxEvent quoteSentForApprovalEvent(Quote quote) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("quoteId", quote.getId());
        payload.put("totalPrice", quote.getTotalPrice());
        return OutboxEvent.create(QUOTE_SENT_FOR_APPROVAL, quote.getServiceOrderId(), payload);
    }
}
