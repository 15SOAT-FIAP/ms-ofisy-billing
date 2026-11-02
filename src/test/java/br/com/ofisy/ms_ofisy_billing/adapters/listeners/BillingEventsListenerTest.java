package br.com.ofisy.ms_ofisy_billing.adapters.listeners;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingEventsListenerTest {

    @Test
    void shouldDispatchTheEventToTheHandlerOfItsType() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        EventHandler stockRefunded = handlerFor("StockRefunded");
        BillingEventsListener listener = new BillingEventsListener(List.of(stockConsumed, stockRefunded));
        EventEnvelope envelope = envelope("StockRefunded");

        listener.onMessage(envelope);

        verify(stockRefunded).handle(envelope);
        verify(stockConsumed, never()).handle(envelope);
    }

    @Test
    void shouldIgnoreAnEventWithoutHandler() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        BillingEventsListener listener = new BillingEventsListener(List.of(stockConsumed));

        listener.onMessage(envelope("StockLow"));

        verify(stockConsumed, never()).handle(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPropagateTheFailureOfAHandlerSoTheMessageIsNotAcknowledged() {
        EventHandler stockConsumed = handlerFor("StockConsumed");
        EventEnvelope envelope = envelope("StockConsumed");
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(stockConsumed).handle(envelope);
        BillingEventsListener listener = new BillingEventsListener(List.of(stockConsumed));

        assertThatThrownBy(() -> listener.onMessage(envelope))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void shouldRefuseTwoHandlersForTheSameType() {
        List<EventHandler> handlers = List.of(handlerFor("StockConsumed"), handlerFor("StockConsumed"));

        assertThatThrownBy(() -> new BillingEventsListener(handlers)).isInstanceOf(IllegalStateException.class);
        assertThat(handlers).hasSize(2);
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
