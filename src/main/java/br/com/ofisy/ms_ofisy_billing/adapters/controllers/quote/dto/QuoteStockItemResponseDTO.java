package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteStockItemResponseDTO(
        UUID id,
        UUID stockId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity
) {}
