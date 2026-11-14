package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventMapperTest {

    @Test
    void shouldConvertDomainToEntity() {
        OutboxEvent event = OutboxEvent.create("QuoteCreated", UUID.randomUUID(), Map.of());
        String payloadJson = "{\"quoteId\":\"%s\"}".formatted(UUID.randomUUID());
        String traceContextJson = "{\"traceparent\":\"00-6af793d30000000025cb7731d653ab37-6d0c2e20f86d8721-01\"}";

        OutboxEventEntity entity = OutboxEventMapper.toEntity(event, payloadJson, traceContextJson);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(event.getEventId());
        assertThat(entity.getEventType()).isEqualTo(event.getEventType());
        assertThat(entity.getServiceOrderId()).isEqualTo(event.getServiceOrderId());
        assertThat(entity.getPayload()).isEqualTo(payloadJson);
        assertThat(entity.getOccurredAt()).isEqualTo(event.getOccurredAt());
        assertThat(entity.getTraceContext()).isEqualTo(traceContextJson);
        assertThat(entity.getPublishedAt()).isNull();
        assertThat(entity.isNew()).isTrue();
    }
}
