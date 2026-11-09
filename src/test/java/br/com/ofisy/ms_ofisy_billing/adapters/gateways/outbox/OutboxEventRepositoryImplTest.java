package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

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

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryImplTest {

    @Mock
    private JpaOutboxEventRepository jpa;

    private OutboxEventRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OutboxEventRepositoryImpl(jpa, JsonMapper.builder().build());
    }

    @Test
    void shouldSerializeThePayloadToJsonAndSaveTheEntity() {
        UUID quoteId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.create("QuoteApproved", UUID.randomUUID(), Map.of("quoteId", quoteId));

        repository.save(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(event.getEventId());
        assertThat(captor.getValue().getEventType()).isEqualTo("QuoteApproved");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"quoteId\":\"" + quoteId + "\"}");
        assertThat(captor.getValue().getPublishedAt()).isNull();
    }
}
