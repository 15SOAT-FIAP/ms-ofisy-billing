package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuoteStockItemMapper {

    public static QuoteStockItemEntity toEntity(QuoteStockItem item) {
        return QuoteStockItemEntity.builder()
                .id(item.getId())
                .stockId(item.getStockId())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static QuoteStockItem toDomain(QuoteStockItemEntity entity) {
        return QuoteStockItem.reconstruct(
                entity.getId(),
                entity.getStockId(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
