package br.com.ofisy.ms_ofisy_billing.application.quote.findbyid;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.util.UUID;

public interface FindQuoteByIdUseCase {

    Quote execute(UUID id);
}
