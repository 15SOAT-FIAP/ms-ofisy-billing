package br.com.ofisy.ms_ofisy_billing.adapters.gateways.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuoteMapper {

    public static QuoteEntity toEntity(Quote quote) {
        QuoteEntity entity = QuoteEntity.builder()
                .id(quote.getId())
                .serviceOrderId(quote.getServiceOrderId())
                .status(quote.getStatus())
                .totalPrice(quote.getTotalPrice())
                .quoteRefusalReason(quote.getQuoteRefusalReason())
                .createdAt(quote.getCreatedAt())
                .updatedAt(quote.getUpdatedAt())
                .build();

        quote.getStockItems().forEach(item -> {
            QuoteStockItemEntity itemEntity = QuoteStockItemMapper.toEntity(item);
            itemEntity.setQuote(entity);
            entity.getStockItems().add(itemEntity);
        });

        quote.getServiceItems().forEach(item -> {
            QuoteServiceItemEntity itemEntity = QuoteServiceItemMapper.toEntity(item);
            itemEntity.setQuote(entity);
            entity.getServiceItems().add(itemEntity);
        });

        return entity;
    }

    public static Quote toDomain(QuoteEntity entity) {
        return Quote.reconstruct(
                entity.getId(),
                entity.getServiceOrderId(),
                entity.getStatus(),
                entity.getTotalPrice(),
                entity.getQuoteRefusalReason(),
                entity.getStockItems().stream().map(QuoteStockItemMapper::toDomain).toList(),
                entity.getServiceItems().stream().map(QuoteServiceItemMapper::toDomain).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
