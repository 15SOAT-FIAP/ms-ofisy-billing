package br.com.ofisy.ms_ofisy_billing.application.quote.findbyid;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindQuoteByIdServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    private FindQuoteByIdService service;

    @BeforeEach
    void setUp() {
        service = new FindQuoteByIdService(quoteRepository);
    }

    @Test
    void shouldReturnTheQuoteWhenItExists() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThat(service.execute(quoteId)).isSameAs(quote);
    }

    @Test
    void shouldFailWhenTheQuoteDoesNotExist() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(quoteId)).isInstanceOf(QuoteNotFoundException.class);
    }
}
