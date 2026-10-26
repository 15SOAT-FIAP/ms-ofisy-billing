package br.com.ofisy.ms_ofisy_billing.adapters.gateways.servicecatalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceCatalogResponse(UUID id, String name, String description, BigDecimal price) {}
