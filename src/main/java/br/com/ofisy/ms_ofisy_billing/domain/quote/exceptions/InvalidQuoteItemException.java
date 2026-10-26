package br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions;

public class InvalidQuoteItemException extends RuntimeException {

    public InvalidQuoteItemException(String message) {
        super(message);
    }
}
