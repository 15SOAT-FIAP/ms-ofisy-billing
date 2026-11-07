package br.com.ofisy.ms_ofisy_billing.domain.processedevent;

import java.util.UUID;

public interface ProcessedEventRepository {

    boolean existsById(UUID eventId);

    ProcessedEvent save(ProcessedEvent processedEvent);
}
