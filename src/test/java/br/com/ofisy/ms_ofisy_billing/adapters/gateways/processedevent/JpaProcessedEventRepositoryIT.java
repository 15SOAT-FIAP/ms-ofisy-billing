package br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent;

import br.com.ofisy.ms_ofisy_billing.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaProcessedEventRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private JpaProcessedEventRepository jpa;

    @Test
    void shouldPersistAndFindTheProcessedEvent() {
        UUID eventId = UUID.randomUUID();

        jpa.saveAndFlush(entity(eventId));

        assertThat(jpa.existsById(eventId)).isTrue();
        assertThat(jpa.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldRejectTheSameEventTwiceByPrimaryKey() {
        UUID eventId = UUID.randomUUID();
        jpa.saveAndFlush(entity(eventId));

        assertThrows(Exception.class, () -> jpa.saveAndFlush(entity(eventId)));
    }

    private ProcessedEventEntity entity(UUID eventId) {
        return ProcessedEventEntity.builder()
                .eventId(eventId)
                .eventType("StockConsumed")
                .processedAt(LocalDateTime.now())
                .build();
    }
}
