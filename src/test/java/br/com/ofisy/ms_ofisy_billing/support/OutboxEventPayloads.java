package br.com.ofisy.ms_ofisy_billing.support;

import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import tools.jackson.databind.json.JsonMapper;

public final class OutboxEventPayloads {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private OutboxEventPayloads() {
    }

    public static String payloadOf(OutboxEvent event) {
        return JSON_MAPPER.writeValueAsString(event.getPayload());
    }
}
