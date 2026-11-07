package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval.SendQuoteForApprovalUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval.SendQuoteForApprovalUseCase.SendQuoteForApprovalCommand;
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
class StockConsumedHandlerTest {

    @Mock
    private SendQuoteForApprovalUseCase useCase;

    @Test
    void shouldHandleStockConsumed() {
        StockConsumedHandler handler = new StockConsumedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "StockConsumed", UUID.randomUUID(), Instant.now(), null);

        assertThat(handler.type()).isEqualTo("StockConsumed");
        handler.handle(envelope);

        verify(useCase).execute(new SendQuoteForApprovalCommand(envelope.eventId(), "StockConsumed", envelope.serviceOrderId()));
    }

    @Test
    void shouldPropagateTheFailureOfTheUseCase() {
        StockConsumedHandler handler = new StockConsumedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "StockConsumed", UUID.randomUUID(), Instant.now(), null);
        doThrow(new IllegalStateException("boom")).when(useCase).execute(any());

        assertThatThrownBy(() -> handler.handle(envelope)).isInstanceOf(IllegalStateException.class);
    }
}
