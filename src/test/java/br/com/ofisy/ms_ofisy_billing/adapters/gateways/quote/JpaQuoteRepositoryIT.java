package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import br.com.ofisy.ms_ofisy_billing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaQuoteRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private JpaQuoteRepository jpaQuoteRepository;

    @Test
    void shouldPersistAQuoteWithItsItemsAndFindItBack() {
        UUID serviceOrderId = UUID.randomUUID();

        QuoteEntity entity = QuoteEntity.builder()
                .serviceOrderId(serviceOrderId)
                .status(QuoteStatus.PENDING)
                .totalPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();

        QuoteStockItemEntity stockItem = QuoteStockItemEntity.builder()
                .stockId(UUID.randomUUID())
                .productName("Filtro de óleo")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(2)
                .createdAt(LocalDateTime.now())
                .build();
        stockItem.setQuote(entity);
        entity.getStockItems().add(stockItem);

        QuoteEntity saved = jpaQuoteRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(jpaQuoteRepository.existsByServiceOrderId(serviceOrderId)).isTrue();

        List<QuoteEntity> found = jpaQuoteRepository.findByServiceOrderId(serviceOrderId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStockItems()).hasSize(1);
        assertThat(found.get(0).getStockItems().get(0).getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(found.get(0).getStockItems().get(0).getProductName()).isEqualTo("Filtro de óleo");
    }

    @Test
    void shouldEnforceOneQuotePerServiceOrder() {
        UUID serviceOrderId = UUID.randomUUID();
        jpaQuoteRepository.save(QuoteEntity.builder()
                .serviceOrderId(serviceOrderId)
                .status(QuoteStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .build());
        jpaQuoteRepository.flush();

        QuoteEntity duplicate = QuoteEntity.builder()
                .serviceOrderId(serviceOrderId)
                .status(QuoteStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jpaQuoteRepository.save(duplicate);
            jpaQuoteRepository.flush();
        });
    }

    @Test
    void shouldNotAllowTheSameStockItemTwiceInTheSameQuote() {
        UUID stockId = UUID.randomUUID();
        QuoteEntity quote = newQuote();
        addStockItem(quote, stockId);
        addStockItem(quote, stockId);

        assertThatThrownBy(() -> jpaQuoteRepository.saveAndFlush(quote))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldNotAllowTheSameServiceItemTwiceInTheSameQuote() {
        UUID serviceCatalogId = UUID.randomUUID();
        QuoteEntity quote = newQuote();
        addServiceItem(quote, serviceCatalogId);
        addServiceItem(quote, serviceCatalogId);

        assertThatThrownBy(() -> jpaQuoteRepository.saveAndFlush(quote))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowTheSameItemsInDifferentQuotes() {
        UUID stockId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        QuoteEntity first = newQuote();
        addStockItem(first, stockId);
        addServiceItem(first, serviceCatalogId);
        QuoteEntity second = newQuote();
        addStockItem(second, stockId);
        addServiceItem(second, serviceCatalogId);

        jpaQuoteRepository.saveAndFlush(first);
        jpaQuoteRepository.saveAndFlush(second);

        assertThat(jpaQuoteRepository.findByServiceOrderId(second.getServiceOrderId())).hasSize(1);
    }

    private QuoteEntity newQuote() {
        return QuoteEntity.builder()
                .serviceOrderId(UUID.randomUUID())
                .status(QuoteStatus.PENDING)
                .totalPrice(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void addStockItem(QuoteEntity quote, UUID stockId) {
        QuoteStockItemEntity item = QuoteStockItemEntity.builder()
                .stockId(stockId)
                .productName("Filtro de óleo")
                .unitPrice(BigDecimal.TEN)
                .quantity(1)
                .createdAt(LocalDateTime.now())
                .build();
        item.setQuote(quote);
        quote.getStockItems().add(item);
    }

    private void addServiceItem(QuoteEntity quote, UUID serviceCatalogId) {
        QuoteServiceItemEntity item = QuoteServiceItemEntity.builder()
                .serviceCatalogId(serviceCatalogId)
                .price(BigDecimal.TEN)
                .createdAt(LocalDateTime.now())
                .build();
        item.setQuote(quote);
        quote.getServiceItems().add(item);
    }
}
