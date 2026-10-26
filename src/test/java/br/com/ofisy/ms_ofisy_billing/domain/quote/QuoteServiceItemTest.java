package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Test
    void shouldRejectNullPrice() {
        assertThatThrownBy(() -> QuoteServiceItem.create(UUID.randomUUID(), null))
                .isInstanceOf(InvalidQuoteItemException.class)
                .hasMessageContaining("Preço");
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() -> QuoteServiceItem.create(UUID.randomUUID(), new BigDecimal("-10.00")))
                .isInstanceOf(InvalidQuoteItemException.class);
    }

    @Test
    void shouldReconstructWithoutValidating() {
        UUID id = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 10, 2, 10, 0);

        QuoteServiceItem item = QuoteServiceItem.reconstruct(id, serviceCatalogId, new BigDecimal("80.00"), createdAt, updatedAt);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getServiceCatalogId()).isEqualTo(serviceCatalogId);
        assertThat(item.getPrice()).isEqualByComparingTo("80.00");
        assertThat(item.getCreatedAt()).isEqualTo(createdAt);
        assertThat(item.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
