package br.com.ofisy.ms_ofisy_billing.adapters.listeners.handlers;

import br.com.ofisy.ms_ofisy_billing.adapters.listeners.EventHandler;
import br.com.ofisy.ms_ofisy_billing.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval.SendQuoteForApprovalUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval.SendQuoteForApprovalUseCase.SendQuoteForApprovalCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsumedHandler implements EventHandler {

    public static final String TYPE = "StockConsumed";

    private final SendQuoteForApprovalUseCase sendQuoteForApprovalUseCase;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void handle(EventEnvelope envelope) {
        sendQuoteForApprovalUseCase.execute(
                new SendQuoteForApprovalCommand(envelope.eventId(), envelope.type(), envelope.serviceOrderId()));
        log.info("StockConsumedHandler.handle >> event {} processed for service order {}",
                envelope.eventId(), envelope.serviceOrderId());
    }
}
