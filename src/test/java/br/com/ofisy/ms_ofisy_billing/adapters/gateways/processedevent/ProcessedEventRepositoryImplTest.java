package br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent;

import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedEventRepositoryImplTest {

    @Mock
    private JpaProcessedEventRepository jpa;

    @InjectMocks
    private ProcessedEventRepositoryImpl repository;

    @Test
    void shouldDelegateTheExistenceCheckToJpa() {
        UUID eventId = UUID.randomUUID();
        when(jpa.existsById(eventId)).thenReturn(true);

        assertThat(repository.existsById(eventId)).isTrue();
    }

    @Test
    void shouldSaveThroughJpaAndReturnTheDomain() {
        UUID eventId = UUID.randomUUID();
        when(jpa.save(any(ProcessedEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedEvent saved = repository.save(ProcessedEvent.create(eventId, "StockConsumed"));

        verify(jpa).save(any(ProcessedEventEntity.class));
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getEventType()).isEqualTo("StockConsumed");
    }
}
