package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Test
    void shouldRejectNullQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, null))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Quantidade");
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, -1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Quantidade");
    }

    @Test
    void shouldRejectNullUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), null, 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Preço unitário");
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Preço unitário");
    }

    @Test
    void shouldReconstructWithoutValidating() {
        UUID id = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 2, 10, 0);

        QuoteStockItem item = QuoteStockItem.reconstruct(id, stockId, new BigDecimal("12.50"), 4, createdAt, updatedAt);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getStockId()).isEqualTo(stockId);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
