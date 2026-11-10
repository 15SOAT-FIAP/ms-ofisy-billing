package br.com.ofisy.ms_ofisy_billing.application.payment.create;

import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase.CreateCheckoutCommand;
import br.com.ofisy.ms_ofisy_billing.application.payment.create.CreatePaymentUseCase.CreatePaymentCommand;
import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
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
import java.util.Arrays;
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
    @Mock
    private CreateCheckoutUseCase createCheckoutUseCase;

    private CreatePaymentService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreatePaymentService(quoteRepository, paymentRepository, processedEventRepository,
                createCheckoutUseCase);
    }

    @Test
    void shouldCreateTheCheckoutWithTheQuoteTotalUsingTheEventIdAsIdempotencyKey() {
        givenAnApprovedQuoteWithPayments();

        service.execute(command());

        ArgumentCaptor<CreateCheckoutCommand> checkout = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
        verify(createCheckoutUseCase).execute(checkout.capture());
        assertThat(checkout.getValue().serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(checkout.getValue().quoteId()).isEqualTo(quoteId);
        assertThat(checkout.getValue().amount()).isEqualByComparingTo("250.00");
        assertThat(checkout.getValue().idempotencyKey()).isEqualTo(eventId.toString());

        ArgumentCaptor<ProcessedEvent> processed = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processed.capture());
        assertThat(processed.getValue().getEventId()).isEqualTo(eventId);
        assertThat(processed.getValue().getEventType()).isEqualTo("ServiceOrderFinished");
    }

    @Test
    void shouldCreateANewCheckoutWhenThePreviousOnesFailedOrExpired() {
        givenAnApprovedQuoteWithPayments(PaymentStatus.FAILED, PaymentStatus.EXPIRED);

        service.execute(command());

        verify(createCheckoutUseCase).execute(any(CreateCheckoutCommand.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldNotCreateAnotherCheckoutWhenTheServiceOrderAlreadyHasAPendingOne() {
        givenAnApprovedQuoteWithPayments(PaymentStatus.PENDING);

        service.execute(command());

        verifyNoInteractions(createCheckoutUseCase);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldNotCreateAnotherCheckoutWhenAFailedOneWasAlreadyReplacedByAPendingOne() {
        givenAnApprovedQuoteWithPayments(PaymentStatus.FAILED, PaymentStatus.PENDING);

        service.execute(command());

        verifyNoInteractions(createCheckoutUseCase);
    }

    @Test
    void shouldNotCreateAnotherCheckoutWhenTheServiceOrderIsAlreadyPaid() {
        givenAnApprovedQuoteWithPayments(PaymentStatus.PAID);

        service.execute(command());

        verifyNoInteractions(createCheckoutUseCase);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void shouldNotMarkTheEventAsProcessedWhenTheCheckoutFails() {
        givenAnApprovedQuoteWithPayments();
        when(createCheckoutUseCase.execute(any()))
                .thenThrow(new PaymentUnavailableException(serviceOrderId, new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(PaymentUnavailableException.class);

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldDiscardAnEventThatWasAlreadyProcessed() {
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.execute(command());

        verifyNoInteractions(quoteRepository, paymentRepository, createCheckoutUseCase);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheQuoteWasNotApproved() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.CANCELLED)));

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(InvalidQuoteStatusException.class);

        verifyNoInteractions(paymentRepository, createCheckoutUseCase);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheServiceOrderHasNoQuote() {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(QuoteNotFoundException.class);

        verifyNoInteractions(paymentRepository, createCheckoutUseCase);
        verify(processedEventRepository, never()).save(any());
    }

    private void givenAnApprovedQuoteWithPayments(PaymentStatus... statuses) {
        when(quoteRepository.findByServiceOrderId(serviceOrderId)).thenReturn(List.of(quote(QuoteStatus.APPROVED)));
        when(paymentRepository.findByServiceOrderId(serviceOrderId))
                .thenReturn(Arrays.stream(statuses).map(this::paymentWithStatus).toList());
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
                "ORD-" + UUID.randomUUID(), null, null, null, null);
    }
}
