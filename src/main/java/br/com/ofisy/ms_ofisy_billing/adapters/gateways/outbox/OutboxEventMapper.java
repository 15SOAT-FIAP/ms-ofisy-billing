package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OutboxEventMapper {

    public static OutboxEventEntity toEntity(OutboxEvent domain, String payloadJson) {
        return OutboxEventEntity.builder()
                .eventId(domain.getEventId())
                .eventType(domain.getEventType())
                .serviceOrderId(domain.getServiceOrderId())
                .payload(payloadJson)
                .occurredAt(domain.getOccurredAt())
                .build();
    }
}
