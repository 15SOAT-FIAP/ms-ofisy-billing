package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockItemRequestDTO(
        @NotNull UUID stockId,
        @NotNull @Min(1) Integer quantity
) {}
