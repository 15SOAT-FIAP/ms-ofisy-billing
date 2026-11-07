package br.com.ofisy.ms_ofisy_billing.application.quote.approve;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.util.UUID;

public interface ApproveQuoteUseCase {

    Quote execute(UUID id);
}
