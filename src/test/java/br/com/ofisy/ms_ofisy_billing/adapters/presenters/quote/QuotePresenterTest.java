package br.com.ofisy.ms_ofisy_billing.adapters.presenters.quote;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.QuoteResponseDTO;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuotePresenterTest {

    @Test
    void shouldPresentQuoteWithItsItems() {
        Quote quote = Quote.create(
                UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));

        QuoteResponseDTO dto = QuotePresenter.present(quote);

        assertThat(dto.serviceOrderId()).isEqualTo(quote.getServiceOrderId());
        assertThat(dto.totalPrice()).isEqualByComparingTo(quote.getTotalPrice());
        assertThat(dto.stockItems()).hasSize(1);
        assertThat(dto.stockItems().get(0).productName()).isEqualTo("Filtro de óleo");
        assertThat(dto.stockItems().get(0).unitPrice()).isEqualByComparingTo("10");
        assertThat(dto.serviceItems()).hasSize(1);
    }
}
