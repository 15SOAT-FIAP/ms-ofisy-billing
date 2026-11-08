package br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions;

import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;

public class InvalidQuoteStatusException extends RuntimeException {

    public InvalidQuoteStatusException(String action, QuoteStatus currentStatus) {
        super("Não é possível " + action + " um orçamento no status " + currentStatus + ".");
    }
}
