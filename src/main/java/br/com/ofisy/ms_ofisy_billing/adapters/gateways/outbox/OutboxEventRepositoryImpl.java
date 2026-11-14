package br.com.ofisy.ms_ofisy_billing.adapters.gateways.outbox;

import br.com.ofisy.ms_ofisy_billing.adapters.tracing.TraceContextPropagator;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final JpaOutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final TraceContextPropagator traceContextPropagator;

    @Override
    public void save(OutboxEvent outboxEvent) {
        String payloadJson = jsonMapper.writeValueAsString(outboxEvent.getPayload());
        outboxEventRepository.save(OutboxEventMapper.toEntity(outboxEvent, payloadJson, currentTraceContextJson()));
    }

    // Roda dentro da requisição (ou do consumo da mensagem), então captura o trace que originou o evento
    private String currentTraceContextJson() {
        Map<String, String> traceContext = traceContextPropagator.captureCurrent();
        return traceContext.isEmpty() ? null : jsonMapper.writeValueAsString(traceContext);
    }
}
