package br.com.ofisy.ms_ofisy_billing.application.quote.update;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ServiceCatalogClient serviceCatalogClient;

    private UpdateQuoteService service;

    @BeforeEach
    void setUp() {
        service = new UpdateQuoteService(quoteRepository, serviceCatalogClient);
    }

    @Test
    void shouldUpdateItemsOfAPendingQuote() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), new BigDecimal("15.00"), 2)),
                List.of());

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldFailWhenQuoteDoesNotExist() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void shouldRejectUpdateWithNoItemsAtAll() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteItemException.class);
    }
}
