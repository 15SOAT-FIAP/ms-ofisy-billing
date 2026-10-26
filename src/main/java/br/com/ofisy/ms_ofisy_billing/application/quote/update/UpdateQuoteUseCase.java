package br.com.ofisy.ms_ofisy_billing.application.quote.update;

import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;

import java.util.List;
import java.util.UUID;

public interface UpdateQuoteUseCase {

    Quote execute(UpdateQuoteCommand cmd);

    record UpdateQuoteCommand(
            UUID quoteId,
            List<CreateQuoteUseCase.StockItemCommand> stockItems,
            List<CreateQuoteUseCase.ServiceItemCommand> serviceItems
    ) {}
}
