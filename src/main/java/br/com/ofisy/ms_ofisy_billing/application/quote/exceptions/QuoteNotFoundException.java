package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(UUID id) {
        super("Orçamento não encontrado: " + id);
    }
}
