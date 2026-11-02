package br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval;

import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.sendforapproval.SendQuoteForApprovalUseCase.SendQuoteForApprovalCommand;
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
class SendQuoteForApprovalServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private SendQuoteForApprovalService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SendQuoteForApprovalService(quoteRepository, processedEventRepository, outboxEventRepository);
    }

    private SendQuoteForApprovalCommand command() {
        return new SendQuoteForApprovalCommand(eventId, "StockConsumed", serviceOrderId);
    }

    private Quote pendingQuote() {
        return Quote.create(serviceOrderId, List.of(QuoteStockItem.create(UUID.randomUUID(), BigDecimal.TEN, 1)), List.of());
    }

    @Test
    void shouldMovePendingQuoteToAwaitingApprovalAndPublishQuoteSentForApproval() {
        Quote quote = pendingQuote();
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote));
        when(quoteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(command());

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.AWAITING_APPROVAL);
        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertThat(outbox.getValue().getEventType()).isEqualTo("QuoteSentForApproval");
        assertThat(outbox.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payloadOf(outbox.getValue())).contains("\"totalPrice\":10");

        ArgumentCaptor<ProcessedEvent> processed = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processed.capture());
        assertThat(processed.getValue().getEventId()).isEqualTo(eventId);
        assertThat(processed.getValue().getEventType()).isEqualTo("StockConsumed");
    }

    @Test
    void shouldDiscardAnEventThatWasAlreadyProcessed() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.execute(command());

        verifyNoInteractions(quoteRepository, outboxEventRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldRecordButIgnoreTheEventWhenTheQuoteIsNoLongerPending() {
        Quote cancelled = Quote.create(serviceOrderId, List.of(), List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
        cancelled.cancel();
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(cancelled));

        service.execute(command());

        assertThat(cancelled.getStatus()).isEqualTo(QuoteStatus.CANCELLED);
        verify(quoteRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldFailWithoutRecordingTheEventWhenTheQuoteDoesNotExist() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(QuoteNotFoundException.class);

        verifyNoInteractions(outboxEventRepository);
        verify(processedEventRepository, never()).save(any());
    }
}
