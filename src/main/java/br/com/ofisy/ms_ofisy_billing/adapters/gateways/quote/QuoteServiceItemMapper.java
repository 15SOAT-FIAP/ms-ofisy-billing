package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuoteServiceItemMapper {

    public static QuoteServiceItemEntity toEntity(QuoteServiceItem item) {
        return QuoteServiceItemEntity.builder()
                .id(item.getId())
                .serviceCatalogId(item.getServiceCatalogId())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public static QuoteServiceItem toDomain(QuoteServiceItemEntity entity) {
        return QuoteServiceItem.reconstruct(
                entity.getId(),
                entity.getServiceCatalogId(),
                entity.getPrice(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
