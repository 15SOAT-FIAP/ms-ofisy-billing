package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteServiceItemResponseDTO(
        UUID id,
        UUID serviceCatalogId,
        BigDecimal price
) {}
