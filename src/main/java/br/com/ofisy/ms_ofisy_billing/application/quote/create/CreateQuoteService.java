package br.com.ofisy.ms_ofisy_billing.application.quote.create;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateQuoteService implements CreateQuoteUseCase {

    private final QuoteRepository quoteRepository;
    private final ServiceCatalogClient serviceCatalogClient;
    private final StockClient stockClient;

    @Override
    @Transactional
    public Quote execute(CreateQuoteCommand cmd) {
        if (quoteRepository.existsByServiceOrderId(cmd.serviceOrderId())) {
            throw new QuoteAlreadyExistsException(cmd.serviceOrderId());
        }

        List<QuoteStockItem> stockItems = buildStockItems(cmd.stockItems() != null ? cmd.stockItems() : List.of());
        List<QuoteServiceItem> serviceItems = buildServiceItems(cmd.serviceItems() != null ? cmd.serviceItems() : List.of());

        Quote quote = Quote.create(cmd.serviceOrderId(), stockItems, serviceItems);
        Quote saved = quoteRepository.save(quote);

        return saved;
    }

    private List<QuoteStockItem> buildStockItems(List<StockItemCommand> commands) {
        List<QuoteStockItem> items = new ArrayList<>();

        for (StockItemCommand command : commands) {
            boolean duplicate = items.stream().anyMatch(i -> i.getStockId().equals(command.stockId()));
            if (duplicate) {
                throw new QuoteItemAlreadyExistsException(command.stockId().toString());
            }
            StockClient.StockView stock = stockClient.findById(command.stockId())
                    .orElseThrow(() -> new StockNotFoundException(command.stockId()));

            items.add(QuoteStockItem.create(command.stockId(), stock.productName(), stock.unitPrice(), command.quantity()));
        }

        return items;
    }

    private List<QuoteServiceItem> buildServiceItems(List<ServiceItemCommand> commands) {
        List<QuoteServiceItem> items = new ArrayList<>();

        for (ServiceItemCommand command : commands) {
            ServiceCatalogClient.ServiceCatalogView catalog = serviceCatalogClient.findById(command.serviceCatalogId())
                    .orElseThrow(() -> new ServiceCatalogNotFoundException(command.serviceCatalogId()));

            boolean duplicate = items.stream()
                    .anyMatch(i -> i.getServiceCatalogId().equals(command.serviceCatalogId()));
            if (duplicate) {
                throw new QuoteItemAlreadyExistsException(catalog.name());
            }

            items.add(QuoteServiceItem.create(command.serviceCatalogId(), catalog.price()));
        }

        return items;
    }
}
