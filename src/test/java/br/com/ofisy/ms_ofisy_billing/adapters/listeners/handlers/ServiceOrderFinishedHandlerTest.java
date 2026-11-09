package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase.CreatePaymentCommand;
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
class ServiceOrderFinishedHandlerTest {

    @Mock
    private CreatePaymentUseCase useCase;

    @Test
    void shouldHandleServiceOrderFinished() {
        ServiceOrderFinishedHandler handler = new ServiceOrderFinishedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "ServiceOrderFinished", UUID.randomUUID(), Instant.now(), null);

        assertThat(handler.type()).isEqualTo("ServiceOrderFinished");
        handler.handle(envelope);

        verify(useCase).execute(new CreatePaymentCommand(envelope.eventId(), "ServiceOrderFinished", envelope.serviceOrderId()));
    }

    @Test
    void shouldPropagateTheFailureOfTheUseCase() {
        ServiceOrderFinishedHandler handler = new ServiceOrderFinishedHandler(useCase);
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID(), "ServiceOrderFinished", UUID.randomUUID(), Instant.now(), null);
        doThrow(new IllegalStateException("boom")).when(useCase).execute(any());

        assertThatThrownBy(() -> handler.handle(envelope)).isInstanceOf(IllegalStateException.class);
    }
}
