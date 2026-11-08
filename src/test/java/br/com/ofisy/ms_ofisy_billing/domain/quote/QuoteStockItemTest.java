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
        QuoteStockItem item = QuoteStockItem.create(stockId, "Filtro de óleo", new BigDecimal("10.00"), 3);

        assertThat(item.getStockId()).isEqualTo(stockId);
        assertThat(item.getProductName()).isEqualTo("Filtro de óleo");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(item.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectNullOrBlankProductName() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), null, BigDecimal.TEN, 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Nome do produto");
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "  ", BigDecimal.TEN, 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Nome do produto");
    }

    @Test
    void shouldRejectZeroOrNegativeQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 0))
                .isInstanceOf(InvalidQuoteItemException.class);
    }

    @Test
    void shouldRejectNullOrNonPositiveUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.ZERO, 1))
                .isInstanceOf(InvalidQuoteItemException.class);
    }

    @Test
    void shouldRejectNullQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, null))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Quantidade");
    }

    @Test
    void shouldRejectNegativeQuantity() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, -1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Quantidade");
    }

    @Test
    void shouldRejectNullUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", null, 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Preço unitário");
    }

    @Test
    void shouldRejectNegativeUnitPrice() {
        assertThatThrownBy(() -> QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", new BigDecimal("-0.01"), 1))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Preço unitário");
    }

    @Test
    void shouldChangeQuantityKeepingTheUnitPriceAndProductName() {
        QuoteStockItem item = QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", new BigDecimal("50.00"), 1);

        item.changeQuantity(4);

        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(item.getProductName()).isEqualTo("Filtro de óleo");
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidQuantityOnChange() {
        QuoteStockItem item = QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", new BigDecimal("50.00"), 1);

        assertThatThrownBy(() -> item.changeQuantity(0)).isInstanceOf(InvalidQuoteItemException.class);
        assertThatThrownBy(() -> item.changeQuantity(null)).isInstanceOf(InvalidQuoteItemException.class);
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    void shouldReconstructWithoutValidating() {
        UUID id = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 2, 10, 0);

        QuoteStockItem item = QuoteStockItem.reconstruct(id, stockId, "Filtro de óleo", new BigDecimal("12.50"), 4, createdAt, updatedAt);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getStockId()).isEqualTo(stockId);
        assertThat(item.getProductName()).isEqualTo("Filtro de óleo");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("12.50");
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
