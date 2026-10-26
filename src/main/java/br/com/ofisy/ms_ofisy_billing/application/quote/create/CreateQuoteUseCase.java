package br.com.ofisy.ms_ofisy_billing.application.quote.create;

import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CreateQuoteUseCase {

    Quote execute(CreateQuoteCommand cmd);

    record CreateQuoteCommand(
            UUID serviceOrderId,
            List<StockItemCommand> stockItems,
            List<ServiceItemCommand> serviceItems
    ) {}

    record StockItemCommand(
            UUID stockId,
            BigDecimal unitPrice,
            Integer quantity
    ) {}

    record ServiceItemCommand(
            UUID serviceCatalogId
    ) {}
}
