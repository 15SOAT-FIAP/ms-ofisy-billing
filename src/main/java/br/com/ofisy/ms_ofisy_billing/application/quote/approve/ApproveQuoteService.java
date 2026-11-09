package br.com.ofisy.ms_ofisy_billing.application.quote.approve;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aprova o orçamento (ação do cliente, rota pública no Gateway — ver ADR-0006/RFC-0007) e
 * publica {@code QuoteApproved}. Este é o evento já consumido pelo ms-ofisy-execution para
 * criar as execuções (ver sua docs/MESSAGING.md): o payload — {@code quoteId} e
 * {@code serviceItems[].serviceCatalogId} — segue exatamente esse contrato. O core também
 * consome este evento para mover a OS para {@code AWAITING_EXECUTION}.
 */
@Service
@RequiredArgsConstructor
public class ApproveQuoteService implements ApproveQuoteUseCase {

    private static final String QUOTE_APPROVED = "QuoteApproved";

    private final QuoteRepository quoteRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public Quote execute(UUID id) {
        Quote quote = quoteRepository.findById(id).orElseThrow(() -> new QuoteNotFoundException(id));
        quote.approve();
        Quote saved = quoteRepository.save(quote);

        outboxEventRepository.save(quoteApprovedEvent(saved));

        return saved;
    }

    private OutboxEvent quoteApprovedEvent(Quote quote) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("quoteId", quote.getId());
        payload.put("serviceItems", quote.getServiceItems().stream()
                .map(item -> Map.of("serviceCatalogId", item.getServiceCatalogId()))
                .toList());
        return OutboxEvent.create(QUOTE_APPROVED, quote.getServiceOrderId(), payload);
    }
}
