package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.*;
import br.com.ofisy.ms_ofisy_billing.adapters.presenters.quote.QuotePresenter;
import br.com.ofisy.ms_ofisy_billing.application.quote.approve.ApproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyid.FindQuoteByIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyserviceorderid.FindQuoteByServiceOrderIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.reprove.ReproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.update.UpdateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController implements QuoteApi {

    private final CreateQuoteUseCase createQuoteUseCase;
    private final UpdateQuoteUseCase updateQuoteUseCase;
    private final ApproveQuoteUseCase approveQuoteUseCase;
    private final ReproveQuoteUseCase reproveQuoteUseCase;
    private final FindQuoteByIdUseCase findQuoteByIdUseCase;
    private final FindQuoteByServiceOrderIdUseCase findQuoteByServiceOrderIdUseCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<QuoteResponseDTO> create(@Valid @RequestBody CreateQuoteRequestDTO request) {
        Quote quote = createQuoteUseCase.execute(new CreateQuoteUseCase.CreateQuoteCommand(
                request.serviceOrderId(), toStockItemCommands(request.stockItems()), toServiceItemCommands(request.serviceItems())));

        return ResponseEntity.status(HttpStatus.CREATED).body(QuotePresenter.present(quote));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<QuoteResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateQuoteRequestDTO request) {
        Quote quote = updateQuoteUseCase.execute(new UpdateQuoteUseCase.UpdateQuoteCommand(
                id, toStockItemCommands(request.stockItems()), toServiceItemCommands(request.serviceItems())));

        return ResponseEntity.ok(QuotePresenter.present(quote));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<QuoteResponseDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(QuotePresenter.present(approveQuoteUseCase.execute(id)));
    }

    @PatchMapping("/{id}/reprove")
    public ResponseEntity<QuoteResponseDTO> reprove(@PathVariable UUID id, @Valid @RequestBody ReproveQuoteRequestDTO request) {
        Quote quote = reproveQuoteUseCase.execute(new ReproveQuoteUseCase.ReproveQuoteCommand(id, request.reason()));
        return ResponseEntity.ok(QuotePresenter.present(quote));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<QuoteResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(QuotePresenter.present(findQuoteByIdUseCase.execute(id)));
    }

    @GetMapping(params = "serviceOrderId")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<List<QuoteResponseDTO>> findByServiceOrderId(@RequestParam UUID serviceOrderId) {
        List<Quote> quotes = findQuoteByServiceOrderIdUseCase.execute(serviceOrderId);
        return ResponseEntity.ok(quotes.stream().map(QuotePresenter::present).toList());
    }

    private List<CreateQuoteUseCase.StockItemCommand> toStockItemCommands(List<StockItemRequestDTO> stockItems) {
        if (stockItems == null) {
            return List.of();
        }
        return stockItems.stream()
                .map(item -> new CreateQuoteUseCase.StockItemCommand(item.stockId(), item.quantity()))
                .toList();
    }

    private List<CreateQuoteUseCase.ServiceItemCommand> toServiceItemCommands(List<ServiceItemRequestDTO> serviceItems) {
        if (serviceItems == null) {
            return List.of();
        }
        return serviceItems.stream()
                .map(item -> new CreateQuoteUseCase.ServiceItemCommand(item.serviceCatalogId()))
                .toList();
    }
}
