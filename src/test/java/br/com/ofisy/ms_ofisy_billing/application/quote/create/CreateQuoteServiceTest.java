package br.com.ofisy.ms_ofisy_billing.application.quote.create;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.StockClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteDataException;


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
class CreateQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ServiceCatalogClient serviceCatalogClient;
    @Mock
    private StockClient stockClient;

    private CreateQuoteService service;

    @BeforeEach
    void setUp() {
        service = new CreateQuoteService(quoteRepository, outboxEventRepository, serviceCatalogClient, stockClient);
    }

    @Test
    void shouldCreateQuoteAndPublishQuoteCreatedWithTheStockItemsToConsume() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();

        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId))
                .thenReturn(Optional.of(new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Troca de óleo", new BigDecimal("120.00"))));
        stubStock(stockId, "Filtro de óleo", "30.00");
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)),
                List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote quote = service.execute(cmd);

        assertThat(quote.getTotalPrice()).isEqualByComparingTo("150.00");
        assertThat(quote.getStatus().name()).isEqualTo("PENDING");
        assertThat(quote.getStockItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("Filtro de óleo");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("30.00");
        });

        ArgumentCaptor<br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("QuoteCreated");
        assertThat(eventCaptor.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payloadOf(eventCaptor.getValue())).contains(stockId.toString()).contains("\"quantity\":1");
    }

    @Test
    void shouldSendQuoteStraightForApprovalWhenItHasOnlyServices() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();

        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId))
                .thenReturn(Optional.of(new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("80.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote quote = service.execute(cmd);

        assertThat(quote.getStatus().name()).isEqualTo("AWAITING_APPROVAL");

        ArgumentCaptor<br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent> eventCaptor =
                ArgumentCaptor.forClass(br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("QuoteSentForApproval");
        assertThat(payloadOf(eventCaptor.getValue())).contains("\"totalPrice\":80");
    }

    @Test
    void shouldRejectWhenQuoteAlreadyExistsForServiceOrder() {
        UUID serviceOrderId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(true);

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(serviceOrderId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteAlreadyExistsException.class);
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldRejectWhenServiceCatalogIsNotFoundInCore() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.empty());

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, List.of(), List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(ServiceCatalogNotFoundException.class);
    }

    @Test
    void shouldRejectDuplicateStockItemsInTheSameQuote() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        stubStock(stockId, "Filtro de óleo", "10.00");

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(
                        new CreateQuoteUseCase.StockItemCommand(stockId, 1),
                        new CreateQuoteUseCase.StockItemCommand(stockId, 2)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteItemAlreadyExistsException.class);
        verify(stockClient, times(1)).findById(stockId);
    }

    @Test
    void shouldRejectWhenStockItemIsNotFoundInTheStock() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(stockClient.findById(stockId)).thenReturn(Optional.empty());

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)), List.of());

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining(stockId.toString());
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldPropagateWhenTheStockIsUnavailable() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(stockClient.findById(stockId)).thenThrow(new StockUnavailableException(stockId, new RuntimeException("timeout")));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 1)), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(StockUnavailableException.class);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldCreateQuoteWithOnlyServiceItemsWhenStockItemsAreNull() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Alinhamento", new BigDecimal("80.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId, null, List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote quote = service.execute(cmd);

        assertThat(quote.getStockItems()).isEmpty();
        assertThat(quote.getServiceItems()).hasSize(1);
        assertThat(quote.getServiceItems().get(0).getPrice()).isEqualByComparingTo("80.00");
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("80.00");
        assertThat(quote.getStatus().name()).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldCreateQuoteWithOnlyStockItemsWhenServiceItemsAreNull() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        stubStock(stockId, "Pastilha de freio", "20.00");
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(new CreateQuoteUseCase.StockItemCommand(stockId, 2)),
                null);

        Quote quote = service.execute(cmd);

        assertThat(quote.getServiceItems()).isEmpty();
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("40.00");
        verifyNoInteractions(serviceCatalogClient);
    }

    @Test
    void shouldRejectQuoteWithoutAnyItemAndNotSaveIt() {
        UUID serviceOrderId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(serviceOrderId, null, null);

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(InvalidQuoteDataException.class);
        verify(quoteRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateServiceItemsInTheSameQuote() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId)).thenReturn(Optional.of(
                new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Troca de óleo", new BigDecimal("120.00"))));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(),
                List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId),
                        new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        assertThatThrownBy(() -> service.execute(cmd))
                .isInstanceOf(QuoteItemAlreadyExistsException.class)
                .hasMessageContaining("Troca de óleo");
        verify(quoteRepository, never()).save(any());
    }

    private void stubStock(UUID stockId, String productName, String unitPrice) {
        when(stockClient.findById(stockId)).thenReturn(Optional.of(
                new StockClient.StockView(stockId, productName, new BigDecimal(unitPrice))));
    }
}
