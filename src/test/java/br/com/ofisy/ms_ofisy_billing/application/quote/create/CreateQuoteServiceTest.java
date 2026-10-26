package br.com.ofisy.ms_ofisy_billing.application.quote.create;

import br.com.ofisy.ms_ofisy_billing.application.ports.ServiceCatalogClient;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteItemAlreadyExistsException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.ServiceCatalogNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateQuoteServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ServiceCatalogClient serviceCatalogClient;

    private CreateQuoteService service;

    @BeforeEach
    void setUp() {
        service = new CreateQuoteService(quoteRepository, serviceCatalogClient);
    }

    @Test
    void shouldCreateQuoteGenerated() {
        UUID serviceOrderId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();

        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(serviceCatalogClient.findById(serviceCatalogId))
                .thenReturn(Optional.of(new ServiceCatalogClient.ServiceCatalogView(serviceCatalogId, "Troca de óleo", new BigDecimal("120.00"))));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), new BigDecimal("30.00"), 1)),
                List.of(new CreateQuoteUseCase.ServiceItemCommand(serviceCatalogId)));

        Quote quote = service.execute(cmd);

        assertThat(quote.getTotalPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldRejectWhenQuoteAlreadyExistsForServiceOrder() {
        UUID serviceOrderId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(true);

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(serviceOrderId, List.of(), List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteAlreadyExistsException.class);
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

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(
                        new CreateQuoteUseCase.StockItemCommand(stockId, BigDecimal.TEN, 1),
                        new CreateQuoteUseCase.StockItemCommand(stockId, BigDecimal.TEN, 2)),
                List.of());

        assertThatThrownBy(() -> service.execute(cmd)).isInstanceOf(QuoteItemAlreadyExistsException.class);
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
    }

    @Test
    void shouldCreateQuoteWithOnlyStockItemsWhenServiceItemsAreNull() {
        UUID serviceOrderId = UUID.randomUUID();
        when(quoteRepository.existsByServiceOrderId(serviceOrderId)).thenReturn(false);
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var cmd = new CreateQuoteUseCase.CreateQuoteCommand(
                serviceOrderId,
                List.of(new CreateQuoteUseCase.StockItemCommand(UUID.randomUUID(), new BigDecimal("20.00"), 2)),
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
}
