package br.com.ofisy.ms_ofisy_billing.application.quote.update;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteItemException;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.ofisy.ms_ofisy_billing.support.OutboxEventPayloads.payloadOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ServiceCatalogClient serviceCatalogClient;
    @Mock
    private StockClient stockClient;

    private UpdateQuoteService service;

    @BeforeEach
    void setUp() {
        service = new UpdateQuoteService(quoteRepository, outboxEventRepository, serviceCatalogClient, stockClient);
    }

    @Test
    void shouldUpdateItemsOfAQuoteAwaitingApprovalAndPublishQuoteUpdated() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        Quote quote = awaitingApprovalQuoteWithOneStockItem();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        stubStock(stockId, "Pastilha de freio", "15.00");
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 2)),
                List.of());

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("30.00");
        assertThat(result.getStockItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("Pastilha de freio");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("15.00");
        });

        assertThat(result.getStatus().name()).isEqualTo("PENDING");

        List<OutboxEvent> events = savedEvents();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("QuoteUpdated");
            assertThat(event.getServiceOrderId()).isEqualTo(quote.getServiceOrderId());
            assertThat(payloadOf(event))
                    .contains(stockId.toString())
                    .contains("\"quantity\":2")
                    .contains("\"totalPrice\":30");
        });
    }

    @Test
    void shouldPublishQuoteUpdatedWithAnEmptyListWhenAllStockItemsAreRemoved() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("90.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote result = service.execute(new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId))));

        assertThat(result.getStatus().name()).isEqualTo("PENDING");
        assertThat(savedEvents()).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("QuoteUpdated");
            assertThat(payloadOf(event)).contains("\"stockItems\":[]").contains("\"totalPrice\":90");
        });
    }

    @Test
    void shouldPublishQuoteUpdatedAlsoWhenAServiceOnlyQuoteIsEdited() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), new BigDecimal("50.00"))));
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("90.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Quote result = service.execute(new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId))));

        assertThat(result.getStatus().name()).isEqualTo("PENDING");
        assertThat(savedEvents()).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("QuoteUpdated");
            assertThat(payloadOf(event)).contains("\"stockItems\":[]");
        });
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
        Quote quote = awaitingApprovalQuoteWithOneStockItem();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteItemException.class);
    }

    @Test
    void shouldUpdateServiceItemsUsingThePriceFromTheCatalog() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
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
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
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
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        stubStock(stockId, "Pastilha de freio", "25.00");
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 2)),
                null);

        Quote result = service.execute(cmd);

        assertThat(result.getTotalPrice()).isEqualByComparingTo("50.00");
        verifyNoInteractions(serviceCatalogClient);
    }

    @Test
    void shouldRejectUpdateWhenBothItemListsAreNull() {
        UUID quoteId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(quoteId, null, null);

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteItemException.class);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldFailWhenServiceCatalogIsNotFoundInCore() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.empty());

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(ServiceCatalogNotFoundException.class);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldRejectDuplicateStockItems() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        stubStock(stockId, "Filtro de óleo", "10.00");

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1),
                        new CreateQuoteUseCase.StockItemCommand(stockId, 2)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(QuoteItemAlreadyExistsException.class)
                .hasMessageContaining(stockId.toString());
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldRejectDuplicateServiceItems() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
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
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldNotUpdateAnApprovedQuote() {
        UUID quoteId = UUID.randomUUID();
        Quote approved = awaitingApprovalQuoteWithOneStockItem();
        approved.approve();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(approved));
        stubStock(stockId, "Filtro de óleo", "10.00");

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteStatusException.class);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldNotUpdateAQuoteWhileTheStockIsStillBeingConsumed() {
        UUID quoteId = UUID.randomUUID();
        Quote pending = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)), List.of());
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(pending));
        stubStock(stockId, "Filtro de óleo", "10.00");

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(InvalidQuoteStatusException.class)
                .hasMessageContaining("PENDING");
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldKeepTheSnapshotOfAnExistingStockItemWithoutQueryingTheStockWhenOnlyTheQuantityChanges() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(stockId, "Filtro de óleo", new BigDecimal("50.00"), 1)), List.of());
        quote.sendForApproval();
        QuoteStockItem original = quote.getStockItems().get(0);
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 3)),
                List.of());

        Quote result = service.execute(cmd);

        assertThat(result.getStockItems()).singleElement().satisfies(item -> {
            assertThat(item).isSameAs(original);
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getUnitPrice()).isEqualByComparingTo("50.00");
            assertThat(item.getProductName()).isEqualTo("Filtro de óleo");
        });
        assertThat(result.getTotalPrice()).isEqualByComparingTo("150.00");
        verifyNoInteractions(stockClient);
    }

    @Test
    void shouldQueryTheStockOnlyForTheNewItemsWhenEditing() {
        UUID quoteId = UUID.randomUUID();
        UUID existingStockId = UUID.randomUUID();
        UUID newStockId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(existingStockId, "Filtro de óleo", new BigDecimal("50.00"), 1)), List.of());
        quote.sendForApproval();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        stubStock(newStockId, "Pastilha de freio", "20.00");
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId,
                List.of(new CreateQuoteUseCase.StockItemCommand(existingStockId, 1),
                        new CreateQuoteUseCase.StockItemCommand(newStockId, 2)),
                List.of());

        Quote result = service.execute(cmd);

        assertThat(result.getStockItems()).extracting(QuoteStockItem::getUnitPrice)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("50.00"), new BigDecimal("20.00"));
        assertThat(result.getStockItems()).extracting(QuoteStockItem::getProductName)
                .containsExactly("Filtro de óleo", "Pastilha de freio");
        assertThat(result.getTotalPrice()).isEqualByComparingTo("90.00");
        verify(stockClient, never()).findById(existingStockId);
    }

    @Test
    void shouldKeepTheSnapshotPriceOfAnExistingServiceItemWhenTheCatalogPriceChanged() {
        UUID quoteId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        Quote quote = Quote.create(UUID.randomUUID(), List.of(),
                List.of(QuoteServiceItem.create(serviceCatalogId, new BigDecimal("100.00"))));
        QuoteServiceItem original = quote.getServiceItems().get(0);
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("150.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote result = service.execute(cmd);

        assertThat(result.getServiceItems()).singleElement().satisfies(item -> {
            assertThat(item).isSameAs(original);
            assertThat(item.getPrice()).isEqualByComparingTo("100.00");
        });
        assertThat(result.getTotalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldFailWhenANewStockItemIsNotFoundInTheStock() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        when(stockClient.findById(stockId)).thenReturn(Optional.empty());

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)), List.of());

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining(stockId.toString());
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldPropagateWhenTheStockIsUnavailable() {
        UUID quoteId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(awaitingApprovalQuoteWithOneStockItem()));
        when(stockClient.findById(stockId)).thenThrow(new StockUnavailableException(stockId, new RuntimeException("timeout")));

        var cmd = new UpdateQuoteUseCase.UpdateQuoteCommand(
                quoteId, List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(StockUnavailableException.class);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    private List<OutboxEvent> savedEvents() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private void stubStock(UUID stockId, String productName, String unitPrice) {
        when(stockClient.findById(stockId)).thenReturn(Optional.of(
                new StockClient.StockView(stockId, productName, new BigDecimal(unitPrice))));
    }

    private Quote awaitingApprovalQuoteWithOneStockItem() {
        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)), List.of());
        quote.sendForApproval();
        return quote;
    }
}
