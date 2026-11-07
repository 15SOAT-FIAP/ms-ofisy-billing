package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(UUID id) {
        super("Orçamento não encontrado: " + id);
    }

    private QuoteNotFoundException(String message) {
        super(message);
    }

    public static QuoteNotFoundException forServiceOrder(UUID serviceOrderId) {
        return new QuoteNotFoundException("Orçamento não encontrado para a OS: " + serviceOrderId);
    }
}
