package br.com.ofisy.ms_ofisy_billing.adapters.controllers.webhook;

import br.com.ofisy.ms_ofisy_billing.application.payment.sync.SyncPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/mercado-pago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private final MercadoPagoSignatureValidator signatureValidator;
    private final SyncPaymentUseCase syncPaymentUseCase;

    @PostMapping
    public ResponseEntity<Void> receive(@RequestParam(name = "data.id", required = false) String dataId,
                                        @RequestHeader(name = "x-signature", required = false) String signature,
                                        @RequestHeader(name = "x-request-id", required = false) String requestId) {
        if (!signatureValidator.isValid(signature, requestId, dataId)) {
            log.warn("MercadoPagoWebhookController.receive >> invalid signature for notification {}", dataId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (dataId == null || dataId.isBlank()) {
            log.info("MercadoPagoWebhookController.receive >> notification without data.id, ignoring");
            return ResponseEntity.ok().build();
        }

        syncPaymentUseCase.execute(dataId);
        return ResponseEntity.ok().build();
    }
}