package br.com.ofisy.ms_ofisy_billing.application.payment.sync;

import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase;
import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase.CreateCheckoutCommand;
import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderStatusResponse;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
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
class SyncPaymentServiceTest {

    private static final String ORDER_ID = "ORDTST01M71SF70T31HXPRCX0A9XHRDE";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private CreateCheckoutUseCase createCheckoutUseCase;

    private SyncPaymentService service;

    private final UUID paymentId = UUID.randomUUID();
    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SyncPaymentService(paymentRepository, outboxEventRepository, paymentClient,
                createCheckoutUseCase);
    }

    @Test
    void shouldConfirmThePaymentWhenTheOrderWasProcessed() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        givenTheOrderIs(PaymentStatus.PAID, "accredited");

        service.execute(ORDER_ID);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);

        OutboxEvent event = savedEvent();
        assertThat(event.getEventType()).isEqualTo("PaymentConfirmed");
        assertThat(event.getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payloadOf(event)).isEqualTo("{\"paymentId\":\"" + paymentId + "\",\"amount\":250.00}");
        verifyNoInteractions(createCheckoutUseCase);
    }

    @Test
    void shouldFailThePaymentAndOpenANewCheckoutWhenTheOrderFailed() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        givenTheOrderIs(PaymentStatus.FAILED, "rejected_by_issuer");

        service.execute(ORDER_ID);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getValue().getStatusDetail()).isEqualTo("rejected_by_issuer");

        OutboxEvent event = savedEvent();
        assertThat(event.getEventType()).isEqualTo("PaymentFailed");
        assertThat(payloadOf(event)).isEqualTo("{\"paymentId\":\"" + paymentId
                + "\",\"reason\":\"FAILED\",\"statusDetail\":\"rejected_by_issuer\"}");

        ArgumentCaptor<CreateCheckoutCommand> checkout = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
        verify(createCheckoutUseCase).execute(checkout.capture());
        assertThat(checkout.getValue().serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(checkout.getValue().quoteId()).isEqualTo(quoteId);
        assertThat(checkout.getValue().amount()).isEqualByComparingTo("250.00");
        assertThat(checkout.getValue().idempotencyKey()).isEqualTo(paymentId.toString());
    }

    @Test
    void shouldExpireThePaymentWithoutOpeningANewCheckout() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        givenTheOrderIs(PaymentStatus.EXPIRED, "expired");

        service.execute(ORDER_ID);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.EXPIRED);

        assertThat(payloadOf(savedEvent())).isEqualTo("{\"paymentId\":\"" + paymentId
                + "\",\"reason\":\"EXPIRED\",\"statusDetail\":\"expired\"}");
        verifyNoInteractions(createCheckoutUseCase);
    }

    @Test
    void shouldDoNothingWhileTheOrderIsStillPending() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        givenTheOrderIs(PaymentStatus.PENDING, "in_process");

        service.execute(ORDER_ID);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository, createCheckoutUseCase);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PAID", "FAILED", "EXPIRED"})
    void shouldIgnoreARepeatedNotificationForAPaymentAlreadyResolved(PaymentStatus status) {
        givenAPaymentWithStatus(status);
        givenTheOrderIs(status, "detail");

        service.execute(ORDER_ID);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository, createCheckoutUseCase);
    }

    @Test
    void shouldNotConfirmAPaymentThatAlreadyFailed() {
        givenAPaymentWithStatus(PaymentStatus.FAILED);
        givenTheOrderIs(PaymentStatus.PAID, "accredited");

        service.execute(ORDER_ID);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository, createCheckoutUseCase);
    }

    @Test
    void shouldIgnoreANotificationForAnUnknownOrder() {
        when(paymentRepository.findByExternalIdForUpdate(ORDER_ID)).thenReturn(Optional.empty());

        service.execute(ORDER_ID);

        verifyNoInteractions(paymentClient, outboxEventRepository, createCheckoutUseCase);
    }

    @Test
    void shouldFailWithoutChangingThePaymentWhenTheProviderIsUnavailable() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        when(paymentClient.getOrder(ORDER_ID))
                .thenThrow(PaymentUnavailableException.forOrderQuery(ORDER_ID, new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.execute(ORDER_ID)).isInstanceOf(PaymentUnavailableException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository, createCheckoutUseCase);
    }

    @Test
    void shouldPropagateTheFailureOfTheNewCheckoutSoTheNotificationIsRetried() {
        givenAPaymentWithStatus(PaymentStatus.PENDING);
        givenTheOrderIs(PaymentStatus.FAILED, "rejected_by_issuer");
        when(createCheckoutUseCase.execute(any()))
                .thenThrow(new PaymentUnavailableException(serviceOrderId, new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.execute(ORDER_ID)).isInstanceOf(PaymentUnavailableException.class);
    }

    private void givenAPaymentWithStatus(PaymentStatus status) {
        when(paymentRepository.findByExternalIdForUpdate(ORDER_ID)).thenReturn(Optional.of(Payment.reconstruct(
                paymentId, serviceOrderId, quoteId, new BigDecimal("250.00"), status, ORDER_ID,
                "https://checkout", null, null, null)));
    }

    private void givenTheOrderIs(PaymentStatus status, String statusDetail) {
        when(paymentClient.getOrder(ORDER_ID)).thenReturn(new PaymentOrderStatusResponse(status, statusDetail));
    }

    private OutboxEvent savedEvent() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        return captor.getValue();
    }
}