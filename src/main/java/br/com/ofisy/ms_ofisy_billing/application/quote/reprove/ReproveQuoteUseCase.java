package br.com.ofisy.ms_ofisy_billing.application.quote.reprove;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.util.UUID;

public interface ReproveQuoteUseCase {

    Quote execute(ReproveQuoteCommand cmd);

    record ReproveQuoteCommand(UUID id, String reason) {}
}
