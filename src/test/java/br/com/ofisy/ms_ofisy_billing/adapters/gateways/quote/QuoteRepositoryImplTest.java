package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteRepositoryImplTest {

    @Mock
    private JpaQuoteRepository jpaQuoteRepository;

    private QuoteRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new QuoteRepositoryImpl(jpaQuoteRepository);
    }

    private Quote newQuote(UUID serviceOrderId) {
        return Quote.create(serviceOrderId,
                List.of(QuoteStockItem.create(UUID.randomUUID(), new BigDecimal("20.00"), 2)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), new BigDecimal("60.00"))));
    }

    @Test
    void shouldMapTheQuoteToAnEntityAndBackWhenSaving() {
        UUID serviceOrderId = UUID.randomUUID();
        Quote quote = newQuote(serviceOrderId);
        when(jpaQuoteRepository.save(any(QuoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quote saved = repository.save(quote);

        ArgumentCaptor<QuoteEntity> captor = ArgumentCaptor.forClass(QuoteEntity.class);
        verify(jpaQuoteRepository).save(captor.capture());
        assertThat(captor.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(captor.getValue().getStockItems()).hasSize(1);
        assertThat(captor.getValue().getServiceItems()).hasSize(1);
        assertThat(saved.getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(saved.getStatus()).isEqualTo(QuoteStatus.PENDING);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldFindAQuoteById() {
        UUID id = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        when(jpaQuoteRepository.findById(id)).thenReturn(Optional.of(QuoteMapper.toEntity(newQuote(serviceOrderId))));

        Optional<Quote> found = repository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getServiceOrderId()).isEqualTo(serviceOrderId);
    }

    @Test
    void shouldReturnEmptyWhenTheQuoteDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(jpaQuoteRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void shouldFindTheQuotesOfAServiceOrder() {
        UUID serviceOrderId = UUID.randomUUID();
        when(jpaQuoteRepository.findByServiceOrderId(serviceOrderId))
                .thenReturn(List.of(QuoteMapper.toEntity(newQuote(serviceOrderId))));

        List<Quote> found = repository.findByServiceOrderId(serviceOrderId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getServiceOrderId()).isEqualTo(serviceOrderId);
    }

    @Test
    void shouldReturnAnEmptyListWhenTheServiceOrderHasNoQuote() {
        UUID serviceOrderId = UUID.randomUUID();
        when(jpaQuoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThat(repository.findByServiceOrderId(serviceOrderId)).isEmpty();
    }

    @Test
    void shouldDelegateTheExistenceCheckByServiceOrder() {
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(jpaQuoteRepository.existsByServiceOrderId(existing)).thenReturn(true);
        when(jpaQuoteRepository.existsByServiceOrderId(missing)).thenReturn(false);

        assertThat(repository.existsByServiceOrderId(existing)).isTrue();
        assertThat(repository.existsByServiceOrderId(missing)).isFalse();
    }
}
