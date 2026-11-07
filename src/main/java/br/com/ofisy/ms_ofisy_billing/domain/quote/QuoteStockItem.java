package br.com.ofisy.ms_ofisy_billing.domain.quote;

import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuoteStockItem {

    private UUID id;
    private Quote quote;
    private UUID stockId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuoteStockItem create(UUID stockId, BigDecimal unitPrice, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidQuoteItemException("Quantidade do item deve ser maior que zero!");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidQuoteItemException("Preço unitário do item de estoque deve ser maior que zero!");
        }
        QuoteStockItem item = new QuoteStockItem();
        item.stockId = stockId;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.createdAt = LocalDateTime.now();
        item.updatedAt = LocalDateTime.now();
        return item;
    }

    public static QuoteStockItem reconstruct(UUID id, UUID stockId, BigDecimal unitPrice,
                                              Integer quantity, LocalDateTime createdAt,
                                              LocalDateTime updatedAt) {
        QuoteStockItem item = new QuoteStockItem();
        item.id = id;
        item.stockId = stockId;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.createdAt = createdAt;
        item.updatedAt = updatedAt;
        return item;
    }

    void setQuote(Quote quote) {
        this.quote = quote;
    }
}
