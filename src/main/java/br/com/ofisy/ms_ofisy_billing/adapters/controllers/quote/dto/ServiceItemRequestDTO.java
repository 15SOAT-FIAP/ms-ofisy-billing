package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServiceItemRequestDTO(
        @NotNull UUID serviceCatalogId
) {}
