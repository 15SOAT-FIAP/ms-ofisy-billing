package br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent;

import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedEventMapperTest {

    @Test
    void shouldMapTheDomainToTheEntityAndBack() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime processedAt = LocalDateTime.of(2026, 10, 20, 10, 0);
        ProcessedEvent domain = ProcessedEvent.reconstruct(eventId, "ServiceOrderFinished", processedAt);

        ProcessedEventEntity entity = ProcessedEventMapper.toEntity(domain);
        ProcessedEvent back = ProcessedEventMapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(eventId);
        assertThat(entity.isNew()).isTrue();
        assertThat(back.getEventId()).isEqualTo(eventId);
        assertThat(back.getEventType()).isEqualTo("ServiceOrderFinished");
        assertThat(back.getProcessedAt()).isEqualTo(processedAt);
    }
}
