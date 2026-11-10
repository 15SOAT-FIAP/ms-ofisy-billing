package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import java.util.List;

public record UpdateQuoteRequestDTO(
        List<StockItemRequestDTO> stockItems,
        List<ServiceItemRequestDTO> serviceItems
) {}
