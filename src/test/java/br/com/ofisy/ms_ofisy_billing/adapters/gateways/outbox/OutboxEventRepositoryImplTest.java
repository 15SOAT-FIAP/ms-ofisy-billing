package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

import br.com.ofisy.ms_ofisy_billing.adapters.tracing.TraceContextPropagator;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryImplTest {

    private static final String TRACEPARENT = "00-6af793d30000000025cb7731d653ab37-6d0c2e20f86d8721-01";

    @Mock
    private JpaOutboxEventRepository jpa;

    @Mock
    private TraceContextPropagator traceContextPropagator;

    private OutboxEventRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OutboxEventRepositoryImpl(jpa, JsonMapper.builder().build(), traceContextPropagator);
    }

    @Test
    void shouldSerializeThePayloadToJsonAndSaveTheEntity() {
        UUID quoteId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("QuoteApproved", UUID.randomUUID(), Map.of("quoteId", quoteId));
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of());

        repository.save(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(event.getEventId());
        assertThat(captor.getValue().getEventType()).isEqualTo("QuoteApproved");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"quoteId\":\"" + quoteId + "\"}");
        assertThat(captor.getValue().getPublishedAt()).isNull();
    }

    @Test
    void shouldSaveTheCurrentTraceContextSerializedAsJson() {
        OutboxEvent event = OutboxEvent.create("QuoteApproved", UUID.randomUUID(), Map.of());
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of("traceparent", TRACEPARENT));

        repository.save(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getTraceContext()).isEqualTo("{\"traceparent\":\"" + TRACEPARENT + "\"}");
    }

    @Test
    void shouldSaveWithoutTraceContextWhenThereIsNoActiveTrace() {
        OutboxEvent event = OutboxEvent.create("QuoteApproved", UUID.randomUUID(), Map.of());
        when(traceContextPropagator.captureCurrent()).thenReturn(Map.of());

        repository.save(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getTraceContext()).isNull();
    }
}
