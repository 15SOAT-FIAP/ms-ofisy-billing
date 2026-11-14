package br.com.ofisy.ms_ofisy_billing.adapters.listeners;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.adapters.tracing.MessageTraceContextReader;
import br.com.ofisy.ms_ofisy_billing.adapters.tracing.TraceContextPropagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingEventsListenerTest {

    private static final String TRACEPARENT = "00-6af79dd30000000039a72b7697c5509c-63cc4ef521ebfd0c-01";

    private final MessageTraceContextReader traceContextReader = mock(MessageTraceContextReader.class);
    private final TraceContextPropagator traceContextPropagator = mock(TraceContextPropagator.class);

    @BeforeEach
    void setUp() {
        when(traceContextReader.read(any())).thenReturn(Map.of());
        doAnswer(invocation -> {
            invocation.getArgument(3, Runnable.class).run();
            return null;
        }).when(traceContextPropagator).runAsConsumer(any(), any(), any(), any());
    }

    @Test
    void shouldDispatchTheEventToTheHandlerOfItsType() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        EventHandler stockRefunded = handlerFor("StockRefunded");
        BillingEventsListener listener = listenerWith(stockConsumed, stockRefunded);
        EventEnvelope envelope = envelope("StockRefunded");

        listener.onMessage(envelope, null);

        verify(stockRefunded).handle(envelope);
        verify(stockConsumed, never()).handle(envelope);
    }

    @Test
    void shouldIgnoreAnEventWithoutHandler() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        BillingEventsListener listener = listenerWith(stockConsumed);

        listener.onMessage(envelope("StockLow"), null);

        verify(stockConsumed, never()).handle(any());
    }

    @Test
    void shouldPropagateTheFailureOfAHandlerSoTheMessageIsNotAcknowledged() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        EventEnvelope envelope = envelope("StockConsumed");
        doThrow(new IllegalStateException("boom")).when(stockConsumed).handle(envelope);
        BillingEventsListener listener = listenerWith(stockConsumed);

        assertThatThrownBy(() -> listener.onMessage(envelope, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void shouldRefuseTwoHandlersForTheSameType() {
        List<EventHandler> handlers = List.of(handlerFor("StockConsumed"), handlerFor("StockConsumed"));

        assertThatThrownBy(() -> new BillingEventsListener(handlers, traceContextReader, traceContextPropagator))
                .isInstanceOf(IllegalStateException.class);
        assertThat(handlers).hasSize(2);
    }

    @Test
    void shouldHandleTheEventInsideTheTraceContextOfTheMessage() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        BillingEventsListener listener = listenerWith(stockConsumed);
        EventEnvelope envelope = envelope("StockConsumed");
        Object traceAttribute = "{\"traceparent\":\"%s\"}".formatted(TRACEPARENT);
        Map<String, String> traceContext = Map.of("traceparent", TRACEPARENT);
        when(traceContextReader.read(traceAttribute)).thenReturn(traceContext);

        listener.onMessage(envelope, traceAttribute);

        ArgumentCaptor<Map<String, String>> attributes = spanAttributesCaptor();
        verify(traceContextPropagator).runAsConsumer(eq(traceContext), eq("event.consume"), attributes.capture(), any());
        assertThat(attributes.getValue())
                .containsEntry("ofisy.event.id", envelope.eventId().toString())
                .containsEntry("ofisy.event.type", "StockConsumed")
                .containsEntry("ofisy.service_order.id", envelope.serviceOrderId().toString());
        verify(stockConsumed).handle(envelope);
    }

    @Test
    void shouldNotTagTheServiceOrderOnTheSpanOfAnEventWithoutServiceOrder() {
        BillingEventsListener listener = listenerWith(handlerFor("StockConsumed"));
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "StockConsumed", null, Instant.now(), null);

        listener.onMessage(envelope, null);

        ArgumentCaptor<Map<String, String>> attributes = spanAttributesCaptor();
        verify(traceContextPropagator).runAsConsumer(any(), any(), attributes.capture(), any());
        assertThat(attributes.getValue()).doesNotContainKey("ofisy.service_order.id");
    }

    private BillingEventsListener listenerWith(EventHandler... handlers) {
        return new BillingEventsListener(List.of(handlers), traceContextReader, traceContextPropagator);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, String>> spanAttributesCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }

    private EventEnvelope envelope(String type) {
        return new EventEnvelope(UUID.randomUUID(), type, UUID.randomUUID(), Instant.now(), null);
    }

    private EventHandler handlerFor(String type) {
        EventHandler handler = mock(EventHandler.class);
        when(handler.type()).thenReturn(type);
        return handler;
    }
}
