package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteMapperTest {

    @Test
    void shouldRoundTripBetweenDomainAndEntityKeepingItemsAndTotal() {
        Quote quote = Quote.create(
                UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), new BigDecimal("20.00"), 2)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), new BigDecimal("60.00"))));

        QuoteEntity entity = QuoteMapper.toEntity(quote);

        assertThat(entity.getServiceOrderId()).isEqualTo(quote.getServiceOrderId());
        assertThat(entity.getTotalPrice()).isEqualByComparingTo(quote.getTotalPrice());
        assertThat(entity.getStockItems()).hasSize(1);
        assertThat(entity.getServiceItems()).hasSize(1);
        assertThat(entity.getStockItems().get(0).getQuote()).isSameAs(entity);

        Quote rebuilt = QuoteMapper.toDomain(entity);

        assertThat(rebuilt.getServiceOrderId()).isEqualTo(quote.getServiceOrderId());
        assertThat(rebuilt.getStatus()).isEqualTo(quote.getStatus());
        assertThat(rebuilt.getStockItems()).hasSize(1);
        assertThat(rebuilt.getServiceItems()).hasSize(1);
    }
}
