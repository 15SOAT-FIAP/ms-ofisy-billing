package br.com.ofisy.ms_ofisy_billing.application.quote.update;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ServiceCatalogClient serviceCatalogClient;

    private UpdateQuoteService service;

    @BeforeEach
    void setUp() {
        service = new UpdateQuoteService(quoteRepository, serviceCatalogClient);
    }

    @Test
    void shouldUpdateItemsOfAPendingQuote() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), new BigDecimal("15.00"), 2)),
                List.of());

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldFailWhenQuoteDoesNotExist() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    void shouldRejectUpdateWithNoItemsAtAll() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteItemException.class);
    }

    private Quote pendingQuoteWithOneStockItem() {
        return Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
    }

    @Test
    void shouldUpdateServiceItemsUsingThePriceFromTheCatalog() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("90.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote result = service.execute(cmd);

        assertThat(result.getStockItems()).isEmpty();
        assertThat(result.getServiceItems()).hasSize(1);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldAcceptNullStockItemsWhenThereAreServiceItems() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("90.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, null, List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void shouldAcceptNullServiceItemsWhenThereAreStockItems() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), new BigDecimal("25.00"), 2)),
                null);

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("50.00");
        verifyNoInteractions(serviceCatalogClient);
    }

    @Test
    void shouldRejectUpdateWhenBothItemListsAreNull() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, null, null);

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteItemException.class);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenServiceCatalogIsNotFoundInCore() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.empty());

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(ServiceCatalogNotFoundException.class);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateStockItems() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, BigDecimal.TEN, 1),
                        new CreateQuoteUseCase.StockItemCommand(stockId, BigDecimal.TEN, 2)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(QuoteItemAlreadyExistsException.class)
                .hasMessageContaining(stockId.toString());
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateServiceItems() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pendingQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Troca de óleo", new BigDecimal("120.00"))));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(),
                List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId),
                        new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(QuoteItemAlreadyExistsException.class)
                .hasMessageContaining("Troca de óleo");
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateAQuoteThatIsNoLongerPending() {
        UUID quoteId = UUID.randomUUID();
        Quote approved = pendingQuoteWithOneStockItem();
        approved.approve();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(approved));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), BigDecimal.TEN, 1)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteStatusException.class);
        verify(quoteRepository, never()).save(any());
    }
}
