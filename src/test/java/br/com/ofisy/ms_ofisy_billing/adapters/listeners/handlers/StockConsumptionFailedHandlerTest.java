package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancelQuoteCommand;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancellationReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockConsumptionFailedHandlerTest {

    @Mock
    private CancelQuoteUseCase useCase;

    @Test
    void shouldHandleStockConsumptionFailed() {
        StockConsumptionFailedHandler handler = new StockConsumptionFailedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "StockConsumptionFailed", UUID.randomUUID(), Instant.now(), null);

        assertThat(handler.type()).isEqualTo("StockConsumptionFailed");
        handler.handle(envelope);

        verify(useCase).execute(new CancelQuoteCommand(envelope.eventId(), "StockConsumptionFailed", envelope.serviceOrderId(), CancellationReason.STOCK_UNAVAILABLE));
    }

    @Test
    void shouldPropagateTheFailureOfTheUseCase() {
        StockConsumptionFailedHandler handler = new StockConsumptionFailedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "StockConsumptionFailed", UUID.randomUUID(), Instant.now(), null);
        doThrow(new IllegalStateException("boom")).when(useCase).execute(any());

        assertThatThrownBy(() -> handler.handle(envelope)).isInstanceOf(IllegalStateException.class);
    }
}
