package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto;

import jakarta.validation.constraints.NotBlank;

public record ReproveQuoteRequestDTO(
        @NotBlank String reason
) {}
