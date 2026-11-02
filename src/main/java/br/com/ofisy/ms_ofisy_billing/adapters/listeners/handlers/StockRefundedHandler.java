package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancelQuoteCommand;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancellationReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockRefundedHandler implements EventHandler {

    public static final String TYPE = "StockRefunded";

    private final CancelQuoteUseCase cancelQuoteUseCase;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(EventEnvelope envelope) {
        cancelQuoteUseCase.execute(new CancelQuoteCommand(
                envelope.eventId(), envelope.type(), envelope.serviceOrderId(), CancellationReason.SERVICE_ORDER_CANCELLED));
        log.info("StockRefundedHandler.handle >> event {} processed for service order {}",
                envelope.eventId(), envelope.serviceOrderId());
    }
}
