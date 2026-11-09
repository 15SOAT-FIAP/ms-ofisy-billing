package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

import java.util.UUID;

public class StockUnavailableException extends RuntimeException {

    public StockUnavailableException(UUID stockId, Throwable cause) {
        super("Não foi possível consultar o item de estoque " + stockId + " no ms-ofisy-stock", cause);
    }
}
