package br.com.ofisy.ms_ofisy_billing.application.quote.exceptions;

import java.util.UUID;

public class ServiceCatalogNotFoundException extends RuntimeException {

    public ServiceCatalogNotFoundException(UUID serviceCatalogId) {
        super("Serviço do catálogo não encontrado: " + serviceCatalogId);
    }
}
