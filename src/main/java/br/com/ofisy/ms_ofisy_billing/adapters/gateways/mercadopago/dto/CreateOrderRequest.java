package br.com.ofisy.ms_ofisy_billing.adapters.gateways.mercadopago.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CreateOrderRequest(
        String type,
        String processingMode,
        String totalAmount,
        String externalReference,
        String expirationTime,
        List<Item> items,
        Config config) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Item(
            String title,
            String unitPrice,
            int quantity
    ) {}

    public record Config(Online online) {}

    public record Online(Retries retries) {}

    public record Retries(boolean allowed) {}
}
