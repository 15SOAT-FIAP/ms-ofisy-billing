package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateQuoteRequestDTO(
        @NotNull UUID serviceOrderId,
        List<StockItemRequestDTO> stockItems,
        List<ServiceItemRequestDTO> serviceItems
) {}
