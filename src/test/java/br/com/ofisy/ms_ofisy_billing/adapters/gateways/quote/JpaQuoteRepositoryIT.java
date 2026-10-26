package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import br.com.ofisy.ms_ofisy_billing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
