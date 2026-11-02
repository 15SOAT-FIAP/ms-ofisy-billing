package br.com.ofisy.ms_ofisy_billing.domain.processedevent;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventTest {

    @Test
    void shouldStampTheProcessingTimeOnCreate() {
        UUID eventId = UUID.randomUUID();

        ProcessedEvent event = ProcessedEvent.create(eventId, "StockConsumed");

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo("StockConsumed");
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldKeepTheStoredValuesOnReconstruct() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime processedAt = LocalDateTime.of(2026, 10, 20, 10, 0);

        ProcessedEvent event = ProcessedEvent.reconstruct(eventId, "StockRefunded", processedAt);

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo("StockRefunded");
        assertThat(event.getProcessedAt()).isEqualTo(processedAt);
    }
}
