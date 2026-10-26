package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteServiceItemTest {

    @Test
    void shouldCreateWithValidPrice() {
        UUID serviceCatalogId = UUID.randomUUID();
        QuoteServiceItem item = QuoteServiceItem.create(serviceCatalogId, new BigDecimal("150.00"));

        assertThat(item.getServiceCatalogId()).isEqualTo(serviceCatalogId);
        assertThat(item.getPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldRejectNonPositivePrice() {
        assertThatThrownBy(() -> QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.ZERO))
                .isInstanceOf(InvalidQuoteItemException.class);
    }
}
