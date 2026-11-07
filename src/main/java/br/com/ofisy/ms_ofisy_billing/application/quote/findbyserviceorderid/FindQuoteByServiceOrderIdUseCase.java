package br.com.ofisy.ms_ofisy_billing.application.quote.findbyserviceorderid;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.util.List;
import java.util.UUID;

public interface FindQuoteByServiceOrderIdUseCase {

    List<Quote> execute(UUID serviceOrderId);
}
