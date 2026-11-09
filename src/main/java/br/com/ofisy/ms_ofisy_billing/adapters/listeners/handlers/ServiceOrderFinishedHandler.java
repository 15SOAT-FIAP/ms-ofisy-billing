package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase.CreatePaymentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceOrderFinishedHandler implements EventHandler {

    public static final String TYPE = "ServiceOrderFinished";

    private final CreatePaymentUseCase createPaymentUseCase;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(EventEnvelope envelope) {
        createPaymentUseCase.execute(
                new CreatePaymentCommand(envelope.eventId(), envelope.type(), envelope.serviceOrderId()));
        log.info("ServiceOrderFinishedHandler.handle >> event {} processed for service order {}",
                envelope.eventId(), envelope.serviceOrderId());
    }
}
