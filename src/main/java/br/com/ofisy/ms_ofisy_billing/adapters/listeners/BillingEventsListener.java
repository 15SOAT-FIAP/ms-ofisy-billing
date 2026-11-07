package br.com.ofisy.ms_ofisy_billing.adapters.listeners;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BillingEventsListener {

    private final Map<String, EventHandler> handlers;

    public BillingEventsListener(List<EventHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(EventHandler::type, Function.identity()));
    }

    @SqsListener("${ofisy.messaging.queues.billing}")
    public void onMessage(EventEnvelope envelope) {
        EventHandler handler = handlers.get(envelope.type());
        if (handler == null) {
            log.warn("BillingEventsListener.onMessage >> ignoring event {} of type {}", envelope.eventId(), envelope.type());
            return;
        }

        handler.handle(envelope);
    }
}
