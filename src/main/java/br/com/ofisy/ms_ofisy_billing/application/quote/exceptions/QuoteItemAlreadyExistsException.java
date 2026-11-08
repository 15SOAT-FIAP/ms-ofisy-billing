package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

public class QuoteItemAlreadyExistsException extends RuntimeException {

    public QuoteItemAlreadyExistsException(String itemName) {
        super("O item '" + itemName + "' já foi adicionado a este orçamento.");
    }
}
