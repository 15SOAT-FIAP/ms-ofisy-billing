package br.com.ofisy.ms_ofisy_billing.domain.outbox;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void shouldCreateEventWithGeneratedIdAndTimestamp() {
        UUID serviceOrderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("quoteId", UUID.randomUUID());

        OutboxEvent event = OutboxEvent.create("QuoteCreated", serviceOrderId, payload);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("QuoteCreated");
        assertThat(event.getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(event.getPayload()).isSameAs(payload);
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    void shouldGenerateADifferentIdForEachEvent() {
        UUID serviceOrderId = UUID.randomUUID();

        OutboxEvent first = OutboxEvent.create("QuoteCreated", serviceOrderId, Map.of());
        OutboxEvent second = OutboxEvent.create("QuoteCreated", serviceOrderId, Map.of());

        assertThat(first.getEventId()).isNotEqualTo(second.getEventId());
    }
}
