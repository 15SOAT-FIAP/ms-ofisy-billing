package br.com.ofisy.ms_ofisy_billing.adapters.presenters.quote;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.QuoteResponseDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.QuoteServiceItemResponseDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.QuoteStockItemResponseDTO;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QuotePresenter {

    public static QuoteResponseDTO present(Quote quote) {
        return new QuoteResponseDTO(
                quote.getId(),
                quote.getServiceOrderId(),
                quote.getStatus(),
                quote.getTotalPrice(),
                quote.getQuoteRefusalReason(),
                quote.getStockItems().stream()
                        .map(item -> new QuoteStockItemResponseDTO(
                                item.getId(), item.getStockId(), item.getProductName(), item.getUnitPrice(),
                                item.getQuantity()))
                        .toList(),
                quote.getServiceItems().stream()
                        .map(item -> new QuoteServiceItemResponseDTO(
                                item.getId(), item.getServiceCatalogId(), item.getPrice()))
                        .toList(),
                quote.getCreatedAt(),
                quote.getUpdatedAt()
        );
    }
}
