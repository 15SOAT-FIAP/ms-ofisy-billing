package br.com.ofisy.ms_ofisy_billing.application.quote.approve;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApproveQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;

    private ApproveQuoteService service;

    @BeforeEach
    void setUp() {
        service = new ApproveQuoteService(quoteRepository);
    }

    @Test
    void shouldApproveQuoteWithServiceCatalogIds() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();

        Quote quote = Quote.create(serviceOrderId, List.of(), List.of(QuoteServiceItem.create(serviceCatalogId, BigDecimal.TEN)));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote result = service.execute(quoteId);

        assertThat(result.getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    void shouldRejectWhenQuoteDoesNotExist() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(quoteId)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void shouldNotApproveAQuoteThatIsNotPending() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(), List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
        quote.reprove("Preço alto");
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.execute(quoteId)).isInstanceOf(InvalidQuoteStatusException.class);
        verify(quoteRepository, never()).save(any());
    }
}
