package br.com.ofisy.ms_ofisy_billing.application.quote.cancel;

import java.util.UUID;

public interface CancelQuoteUseCase {

    void execute(CancelQuoteCommand cmd);

    record CancelQuoteCommand(UUID eventId, String eventType, UUID serviceOrderId, CancellationReason reason) {}

    enum CancellationReason {
        STOCK_UNAVAILABLE,
        SERVICE_ORDER_CANCELLED
    }
}
