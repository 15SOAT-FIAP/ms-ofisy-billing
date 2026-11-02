package br.com.ofisy.ms_ofisy_billing.adapters.gateways.processedevent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
