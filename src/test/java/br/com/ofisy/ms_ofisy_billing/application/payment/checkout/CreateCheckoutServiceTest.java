package br.com.ofisy.ms_ofisy_billing.application.payment.checkout;

import br.com.ofisy.ms_ofisy_billing.application.payment.checkout.CreateCheckoutUseCase.CreateCheckoutCommand;
import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderRequest;
import br.com.ofisy.ms_ofisy_billing.application.ports.PaymentClient.PaymentOrderResponse;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEvent;
import br.com.ofisy.ms_ofisy_billing.domain.outbox.OutboxEventRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class CreateCheckoutServiceTest {

    private static final String ORDER_ID = "ORDTST01KS5AJ6HTK2HRQ3XJ3C2JCKP9";
    private static final String CHECKOUT_URL =
            "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=" + ORDER_ID;
    private static final String IDEMPOTENCY_KEY = "idempotency-key";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private PaymentClient paymentClient;

    private CreateCheckoutService service;

    private final UUID serviceOrderId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateCheckoutService(paymentRepository, outboxEventRepository, paymentClient);
    }

    @Test
    void shouldCreateTheOrderInTheProviderWithTheGivenIdempotencyKey() {
        givenTheProviderCreatesTheOrder();
        givenTheRepositorySavesThePayment();

        service.execute(command());

        ArgumentCaptor<PaymentOrderRequest> request = ArgumentCaptor.forClass(PaymentOrderRequest.class);
        verify(paymentClient).createOrder(request.capture());
        assertThat(request.getValue().idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(request.getValue().serviceOrderId()).isEqualTo(serviceOrderId);
        assertThat(request.getValue().amount()).isEqualByComparingTo("250.00");
    }

    @Test
    void shouldSaveAPendingPaymentWithTheOrderIdAndCheckoutUrl() {
        givenTheProviderCreatesTheOrder();
        givenTheRepositorySavesThePayment();

        Payment result = service.execute(command());

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payment.getValue().getQuoteId()).isEqualTo(quoteId);
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("250.00");
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getValue().getExternalId()).isEqualTo(ORDER_ID);
        assertThat(payment.getValue().getCheckoutUrl()).isEqualTo(CHECKOUT_URL);
        assertThat(result.getId()).isEqualTo(paymentId);
    }

    @Test
    void shouldPublishPaymentCheckoutCreatedWithTheCheckoutUrl() {
        givenTheProviderCreatesTheOrder();
        givenTheRepositorySavesThePayment();

        service.execute(command());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PaymentCheckoutCreated");
        assertThat(captor.getValue().getServiceOrderId()).isEqualTo(serviceOrderId);
        assertThat(payloadOf(captor.getValue()))
                .isEqualTo("{\"paymentId\":\"" + paymentId + "\",\"checkoutUrl\":\"" + CHECKOUT_URL
                        + "\",\"amount\":250.00}");
    }

    @Test
    void shouldFailWithoutSavingWhenTheProviderIsUnavailable() {
        when(paymentClient.createOrder(any()))
                .thenThrow(new PaymentUnavailableException(serviceOrderId, new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(PaymentUnavailableException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldFailWithoutPublishingWhenSavingFailsAfterTheOrderWasCreated() {
        givenTheProviderCreatesTheOrder();
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("database down"));

        assertThatThrownBy(() -> service.execute(command())).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(outboxEventRepository);
    }

    private CreateCheckoutCommand command() {
        return new CreateCheckoutCommand(serviceOrderId, quoteId, new BigDecimal("250.00"), IDEMPOTENCY_KEY);
    }

    private void givenTheProviderCreatesTheOrder() {
        when(paymentClient.createOrder(any())).thenReturn(new PaymentOrderResponse(ORDER_ID, CHECKOUT_URL));
    }

    private void givenTheRepositorySavesThePayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment payment = inv.getArgument(0);
            return Payment.reconstruct(paymentId, payment.getServiceOrderId(), payment.getQuoteId(),
                    payment.getAmount(), payment.getStatus(), payment.getExternalId(), payment.getCheckoutUrl(),
                    payment.getStatusDetail(), payment.getCreatedAt(), payment.getUpdatedAt());
        });
    }
}
