package br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval;

import java.util.UUID;

public interface SendQuoteForApprovalUseCase {

    void execute(SendQuoteForApprovalCommand cmd);

    record SendQuoteForApprovalCommand(UUID eventId, String eventType, UUID serviceOrderId) {}
}
