package br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderResponse(String id, String status, String checkoutUrl) {}
