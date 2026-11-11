package br.com.ofisy.ms_ofisy_billing.adapters.controllers.webhook;

import br.com.ofisy.ms_ofisy_billing.application.payment.exceptions.PaymentUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.payment.sync.SyncPaymentUseCase;
import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MercadoPagoWebhookController.class)
class MercadoPagoWebhookControllerTest {

    private static final String URL = "/api/v1/webhooks/mercado-pago";
    private static final String ORDER_ID = "ORD01M28P44G5FG8RJPM579EH56FV";
    private static final String SIGNATURE = "ts=1742505638683,v1=abc";
    private static final String REQUEST_ID = "2066ca19-c6f1-498a-be75-1923005edd06";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private MercadoPagoSignatureValidator signatureValidator;
    @MockitoBean
    private SyncPaymentUseCase syncPaymentUseCase;

    @Test
    void shouldSyncThePaymentOfTheNotifiedOrder() throws Exception {
        when(signatureValidator.isValid(SIGNATURE, REQUEST_ID, ORDER_ID)).thenReturn(true);

        mockMvc.perform(post(URL).queryParam("data.id", ORDER_ID).queryParam("type", "order")
                        .header("x-signature", SIGNATURE).header("x-request-id", REQUEST_ID)
                        .contentType("application/json").content("{\"action\":\"order.processed\"}"))
                .andExpect(status().isOk());

        verify(syncPaymentUseCase).execute(ORDER_ID);
    }

    @Test
    void shouldRejectANotificationWithAnInvalidSignature() throws Exception {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(false);

        mockMvc.perform(post(URL).queryParam("data.id", ORDER_ID)
                        .header("x-signature", SIGNATURE).header("x-request-id", REQUEST_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(syncPaymentUseCase);
    }

    @Test
    void shouldPropagateASyncFailureSoTheProviderRetriesTheNotification() {
        when(signatureValidator.isValid(SIGNATURE, REQUEST_ID, ORDER_ID)).thenReturn(true);
        doThrow(PaymentUnavailableException.forOrderQuery(ORDER_ID, new RuntimeException("timeout")))
                .when(syncPaymentUseCase).execute(ORDER_ID);

        assertThatThrownBy(() -> mockMvc.perform(post(URL).queryParam("data.id", ORDER_ID)
                        .header("x-signature", SIGNATURE).header("x-request-id", REQUEST_ID)))
                .hasCauseInstanceOf(PaymentUnavailableException.class);
    }

    @Test
    void shouldAcknowledgeANotificationWithoutDataIdWithoutSyncing() throws Exception {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);

        mockMvc.perform(post(URL).header("x-signature", SIGNATURE).header("x-request-id", REQUEST_ID))
                .andExpect(status().isOk());

        verifyNoInteractions(syncPaymentUseCase);
    }
}