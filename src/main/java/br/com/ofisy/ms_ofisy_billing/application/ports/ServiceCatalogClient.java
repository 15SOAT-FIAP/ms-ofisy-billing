package br.com.ofisy.ms_ofisy_billing.application.ports;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCatalogClient {

    Optional<ServiceCatalogView> findById(UUID serviceCatalogId);

    record ServiceCatalogView(UUID id, String name, BigDecimal price) {}
}
