package br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent;

import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpa;

    @Override
    public boolean existsById(UUID eventId) {
        return jpa.existsById(eventId);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent processedEvent) {
        ProcessedEventEntity saved = jpa.save(ProcessedEventMapper.toEntity(processedEvent));
        return ProcessedEventMapper.toDomain(saved);
    }
}
