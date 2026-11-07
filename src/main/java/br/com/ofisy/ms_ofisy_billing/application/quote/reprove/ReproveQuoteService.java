package br.com.ofisy.ms_ofisy_billing.application.quote.reprove;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReproveQuoteService implements ReproveQuoteUseCase {

    private static final String QUOTE_REPROVED = "QuoteReproved";

    private final QuoteRepository quoteRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public Quote execute(ReproveQuoteCommand cmd) {
        Quote quote = quoteRepository.findById(cmd.id()).orElseThrow(() -> new QuoteNotFoundException(cmd.id()));
        quote.reprove(cmd.reason());
        Quote saved = quoteRepository.save(quote);

        outboxEventRepository.save(quoteReprovedEvent(saved));

        return saved;
    }

    /** O ms-ofisy-stock devolve os itens (por isso {@code stockItems}) e o core cancela a OS. */
    private OutboxEvent quoteReprovedEvent(Quote quote) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("quoteId", quote.getId());
        payload.put("reason", quote.getQuoteRefusalReason());
        payload.put("stockItems", stockItemsPayload(quote));
        return OutboxEvent.create(QUOTE_REPROVED, quote.getServiceOrderId(), payload);
    }

    private List<Map<String, Object>> stockItemsPayload(Quote quote) {
        return quote.getStockItems().stream()
                .map(item -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("stockId", item.getStockId());
                    entry.put("quantity", item.getQuantity());
                    return entry;
                })
                .toList();
    }
}
