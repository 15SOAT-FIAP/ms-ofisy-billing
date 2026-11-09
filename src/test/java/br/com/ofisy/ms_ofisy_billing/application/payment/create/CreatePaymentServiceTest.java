package br.com.ofisy.ms_ofisy_billing.application.payment.create;

import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase.CreatePaymentCommand;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEvent;
import br.com.ofisy.ms_ofisy_billing.domain.processedevent.ProcessedEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteRepository;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStatus;
import br.com.ofisy.ms_ofisy_billing.domain.quote.exceptions.InvalidQuoteStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaymentServiceTest {

    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private CreatePaymentService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreatePaymentService(quoteRepository, paymentRepository, processedEventRepository);
    }

    @Test
    void shouldCreateAPendingPaymentWithTheTotalOfTheApprovedQuote() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.APPROVED)));
        when(paymentRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        service.execute(command());

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payment.getValue().getQuoteId()).isEqualTo(quoteId);
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("250.00");
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getValue().getExternalId()).isNull();
        assertThat(payment.getValue().getCheckoutUrl()).isNull();

        ArgumentCaptor<ProcessedEvent> processed = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processed.capture());
        assertThat(processed.getValue().getEventId()).isEqualTo(eventId);
        assertThat(processed.getValue().getEventType()).isEqualTo("ServiceOrderFinished");
    }

    @Test
    void shouldCreateANewPaymentWhenThePreviousOneWasRejected() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.APPROVED)));
        when(paymentRepository.findByServiceOrderId(serviceOrderId))
                .thenReturn(List.of(paymentWithStatus(PaymentStatus.REJECTED)));

        service.execute(command());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldNotCreateAnotherPaymentWhenTheServiceOrderAlreadyHasOne() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.APPROVED)));
        when(paymentRepository.findByServiceOrderId(serviceOrderId))
                .thenReturn(List.of(paymentWithStatus(PaymentStatus.PENDING)));

        service.execute(command());

        verify(paymentRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldDiscardAnEventThatWasAlreadyProcessed() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.execute(command());

        verifyNoInteractions(quoteRepository, paymentRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheQuoteWasNotApproved() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.CANCELLED)));

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(InvalidQuoteStatusException.class);

        verifyNoInteractions(paymentRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheServiceOrderHasNoQuote() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(QuoteNotFoundException.class);

        verifyNoInteractions(paymentRepository);
        verify(processedEventRepository, never()).save(any());
    }

    private CreatePaymentCommand command() {
        return new CreatePaymentCommand(eventId, "ServiceOrderFinished", serviceOrderId);
    }

    private Quote quote(QuoteStatus status) {
        return Quote.reconstruct(quoteId, serviceOrderId, status, new BigDecimal("250.00"), null,
                List.of(), List.of(), null, null);
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        return Payment.reconstruct(UUID.randomUUID(), serviceOrderId, quoteId, new BigDecimal("250.00"), status,
                null, null, null, null);
    }
}
