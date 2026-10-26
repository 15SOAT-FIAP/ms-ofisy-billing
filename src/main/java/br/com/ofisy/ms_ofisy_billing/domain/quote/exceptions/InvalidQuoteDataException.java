package br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions;

import java.util.UUID;

public class InvalidQuoteDataException extends RuntimeException {

    public InvalidQuoteDataException(UUID serviceOrderId) {
        super("O orçamento da ordem de serviço " + serviceOrderId
                + " deve conter ao menos um item de estoque ou serviço.");
    }
}
