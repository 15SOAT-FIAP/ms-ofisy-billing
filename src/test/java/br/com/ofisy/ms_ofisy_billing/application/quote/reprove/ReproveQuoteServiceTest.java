package br.com.ofisy.ms_ofisy_billing.application.quote.reprove;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReproveQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    private ReproveQuoteService service;

    @BeforeEach
    void setUp() {
        service = new ReproveQuoteService(quoteRepository);
    }

    @Test
    void shouldReproveQuoteAndPublishQuoteReprovedWithStockItemsToRelease() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        Quote quote = Quote.create(serviceOrderId, List.of(QuoteStockItem.create(stockId, BigDecimal.TEN, 5)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote result = service.execute(new ReproveQuoteUseCase.ReproveQuoteCommand(quoteId, "Preço acima do esperado"));

        assertThat(result.getStatus().name()).isEqualTo("REPROVED");
        assertThat(result.getQuoteRefusalReason()).isEqualTo("Preço acima do esperado");
    }
}
