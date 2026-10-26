package br.com.ofisy.ms_ofisy_billing.application.quote.findbyserviceorderid;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindQuoteByServiceOrderIdServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    private FindQuoteByServiceOrderIdService service;

    @BeforeEach
    void setUp() {
        service = new FindQuoteByServiceOrderIdService(quoteRepository);
    }

    @Test
    void shouldReturnTheQuotesOfTheServiceOrder() {
        UUID serviceOrderId = UUID.randomUUID();
        Quote quote = Quote.create(serviceOrderId,
                List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote));

        List<Quote> result = service.execute(serviceOrderId);

        assertThat(result).containsExactly(quote);
    }

    @Test
    void shouldReturnAnEmptyListWhenTheServiceOrderHasNoQuote() {
        UUID serviceOrderId = UUID.randomUUID();
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThat(service.execute(serviceOrderId)).isEmpty();
    }
}
