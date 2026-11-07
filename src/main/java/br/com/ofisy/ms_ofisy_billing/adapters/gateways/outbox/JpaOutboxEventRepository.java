package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.UUID;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<OutboxEventEntity> findByPublishedAtIsNullOrderByOccurredAtAsc(Limit limit);
}
