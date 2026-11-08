package br.com.ofisy.ms_ofisy_billing.application.quote.update;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateQuoteService implements UpdateQuoteUseCase {

    private final QuoteRepository quoteRepository;
    private final ServiceCatalogClient serviceCatalogClient;
    private final StockClient stockClient;

    @Override
    @Transactional
    public Quote execute(UpdateQuoteCommand cmd) {
        Quote quote = quoteRepository.findById(cmd.quoteId())
                .orElseThrow(() -> new QuoteNotFoundException(cmd.quoteId()));

        var stockCommands = cmd.stockItems() != null ? cmd.stockItems() : List.<CreateQuoteUseCase.StockItemCommand>of();
        var serviceCommands = cmd.serviceItems() != null ? cmd.serviceItems() : List.<CreateQuoteUseCase.ServiceItemCommand>of();

        if (stockCommands.isEmpty() && serviceCommands.isEmpty()) {
            throw new InvalidQuoteItemException("O orçamento deve conter ao menos um item de estoque ou serviço.");
        }

        List<QuoteStockItem> stockItems = buildStockItems(quote, stockCommands);
        List<QuoteServiceItem> serviceItems = buildServiceItems(quote, serviceCommands);

        quote.update(stockItems, serviceItems);
        return quoteRepository.save(quote);
    }

    private List<QuoteStockItem> buildStockItems(Quote quote, List<CreateQuoteUseCase.StockItemCommand> commands) {
        Map<UUID, QuoteStockItem> existingByStockId = quote.getStockItems().stream()
                .collect(Collectors.toMap(QuoteStockItem::getStockId, Function.identity()));

        List<QuoteStockItem> items = new ArrayList<>();
        Set<UUID> requestedStockIds = new HashSet<>();

        for (CreateQuoteUseCase.StockItemCommand command : commands) {
            boolean duplicate = !requestedStockIds.add(command.stockId());
            if (duplicate) {
                throw new QuoteItemAlreadyExistsException(command.stockId().toString());
            }

            QuoteStockItem existing = existingByStockId.get(command.stockId());
            if (existing != null) {
                existing.changeQuantity(command.quantity());
                items.add(existing);
            } else {
                StockClient.StockView stock = stockClient.findById(command.stockId())
                        .orElseThrow(() -> new StockNotFoundException(command.stockId()));
                items.add(QuoteStockItem.create(command.stockId(), stock.productName(), stock.unitPrice(), command.quantity()));
            }
        }

        return items;
    }

    private List<QuoteServiceItem> buildServiceItems(Quote quote, List<CreateQuoteUseCase.ServiceItemCommand> commands) {
        Map<UUID, QuoteServiceItem> existingByServiceCatalogId = quote.getServiceItems().stream()
                .collect(Collectors.toMap(QuoteServiceItem::getServiceCatalogId, Function.identity()));

        List<QuoteServiceItem> items = new ArrayList<>();
        Set<UUID> requestedCatalogIds = new HashSet<>();

        for (CreateQuoteUseCase.ServiceItemCommand command : commands) {
            ServiceCatalogClient.ServiceCatalogView catalog = serviceCatalogClient.findById(command.serviceCatalogId())
                    .orElseThrow(() -> new ServiceCatalogNotFoundException(command.serviceCatalogId()));

            boolean duplicate = !requestedCatalogIds.add(command.serviceCatalogId());
            if (duplicate) {
                throw new QuoteItemAlreadyExistsException(catalog.name());
            }

            QuoteServiceItem existing = existingByServiceCatalogId.get(command.serviceCatalogId());
            items.add(existing != null ? existing : QuoteServiceItem.create(command.serviceCatalogId(), catalog.price()));
        }

        return items;
    }
}
