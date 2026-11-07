package br.com.ofisy.ms_ofisy_billing.application.quote.cancel;

import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancelQuoteCommand;
import br.com.ofisy.ms_ofisy_billing.application.quote.cancel.CancelQuoteUseCase.CancellationReason;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static br.com.ofisy.ms_ofisy_billing.support.OutboxEventPayloads.payloadOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelQuoteServiceTest {

    private final UUID eventId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private CancelQuoteService service;

    @BeforeEach
    void setUp() {
        service = new CancelQuoteService(quoteRepository, processedEventRepository, outboxEventRepository);
    }

    @Test
    void shouldCancelPendingQuoteWhenStockConsumptionFails() {
        Quote quote = pendingQuote();
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(command("StockConsumptionFailed", CancellationReason.STOCK_UNAVAILABLE));

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CANCELLED);
        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertThat(outbox.getValue().getEventType()).isEqualTo("QuoteCancelled");
        assertThat(outbox.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payloadOf(outbox.getValue())).contains("\"reason\":\"STOCK_UNAVAILABLE\"");

        ArgumentCaptor<ProcessedEvent> processed = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processed.capture());
        assertThat(processed.getValue().getEventId()).isEqualTo(eventId);
        assertThat(processed.getValue().getEventType()).isEqualTo("StockConsumptionFailed");
    }

    @Test
    void shouldCancelApprovedQuoteWhenStockIsRefundedAfterTheServiceOrderWasCancelled() {
        Quote quote = serviceOnlyQuote();
        quote.approve();
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(command("StockRefunded", CancellationReason.SERVICE_ORDER_CANCELLED));

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CANCELLED);
        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertThat(payloadOf(outbox.getValue())).contains("\"reason\":\"SERVICE_ORDER_CANCELLED\"");
    }

    @Test
    void shouldNotEmitASecondCancellationWhenTheQuoteWasAlreadyReproved() {
        Quote quote = serviceOnlyQuote();
        quote.reprove("Preço alto");
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote));

        service.execute(command("StockRefunded", CancellationReason.SERVICE_ORDER_CANCELLED));

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.REPROVED);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldDiscardAnEventThatWasAlreadyProcessed() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.execute(command("StockRefunded", CancellationReason.SERVICE_ORDER_CANCELLED));

        verifyNoInteractions(quoteRepository, outboxEventRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldFailWithoutRecordingTheEventWhenTheQuoteDoesNotExistAfterAStockFailure() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.execute(command("StockConsumptionFailed", CancellationReason.STOCK_UNAVAILABLE)))
                .isInstanceOf(QuoteNotFoundException.class);

        verifyNoInteractions(outboxEventRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldOnlyRecordTheEventWhenTheServiceOrderWasCancelledBeforeAnyQuoteExisted() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        service.execute(command("StockRefunded", CancellationReason.SERVICE_ORDER_CANCELLED));

        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    private CancelQuoteCommand command(String type, CancellationReason reason) {
        return new CancelQuoteCommand(eventId, type, serviceOrderId, reason);
    }

    private Quote pendingQuote() {
        return Quote.create(serviceOrderId, List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
    }

    private Quote serviceOnlyQuote() {
        return Quote.create(serviceOrderId, List.of(), List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
    }
}
