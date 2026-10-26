package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteStockItemTest {

    @Test
    void shouldCreateWithValidData() {
        UUID stockId = UUID.randomUUID();
        QuoteStockItem item = QuoteStockItem.create(stockId, new BigDecimal("10.00"), 3);

        assertThat(item.getStockId()).isEqualTo(stockId);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 0))
                .isInstanceOf(InvalidQuoteItemException.class);
    }

    @Test
    void shouldRejectNullOrNonPositiveUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidQuoteItemException.class);
    }
}
