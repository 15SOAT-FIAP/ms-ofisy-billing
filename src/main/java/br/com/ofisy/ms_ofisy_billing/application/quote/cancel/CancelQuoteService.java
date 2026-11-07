package br.com.ofisy.ms_ofisy_billing.application.quote.cancel;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
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
public class CancelQuoteService implements CancelQuoteUseCase {

    private static final String QUOTE_CANCELLED = "QuoteCancelled";

    private final QuoteRepository quoteRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    public void execute(CancelQuoteCommand cmd) {
        if (processedEventRepository.existsById(cmd.eventId())) {
            return;
        }

        Optional<Quote> found = quoteRepository.findByServiceOrderId(cmd.serviceOrderId()).stream().findFirst();
        if (found.isEmpty()) {
            if (CancellationReason.SERVICE_ORDER_CANCELLED.equals(cmd.reason())) {
                log.info("CancelQuoteService.execute >> service order {} has no quote, nothing to cancel for event {}",
                        cmd.serviceOrderId(), cmd.eventId());
                processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
                return;
            }
            throw QuoteNotFoundException.forServiceOrder(cmd.serviceOrderId());
        }
        Quote quote = found.get();

        if (quote.isCancellable()) {
            quote.cancel();
            Quote saved = quoteRepository.save(quote);
            outboxEventRepository.save(quoteCancelledEvent(saved, cmd.reason().name()));
        } else {
            log.info("CancelQuoteService.execute >> quote {} is {}, nothing to cancel for event {}",
                    quote.getId(), quote.getStatus(), cmd.eventId());
        }

        processedEventRepository.save(ProcessedEvent.create(cmd.eventId(), cmd.eventType()));
    }

    private OutboxEvent quoteCancelledEvent(Quote quote, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("quoteId", quote.getId());
        payload.put("reason", reason);
        return OutboxEvent.create(QUOTE_CANCELLED, quote.getServiceOrderId(), payload);
    }
}
