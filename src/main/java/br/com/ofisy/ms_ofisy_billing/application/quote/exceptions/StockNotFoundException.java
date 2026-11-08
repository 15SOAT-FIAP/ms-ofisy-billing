package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(UUID stockId) {
        super("Item de estoque não encontrado: " + stockId);
    }
}
