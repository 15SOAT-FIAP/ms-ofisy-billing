package br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment.dto.PaymentResponseDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.presenters.payment.PaymentPresenter;
import br.com.ofisy.ms_ofisy_billing.application.payment.findbyserviceorderid.FindPaymentByServiceOrderIdUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final FindPaymentByServiceOrderIdUseCase findPaymentByServiceOrderIdUseCase;

    @GetMapping(params = "serviceOrderId")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<List<PaymentResponseDTO>> findByServiceOrderId(@RequestParam UUID serviceOrderId) {
        var payments = findPaymentByServiceOrderIdUseCase.execute(serviceOrderId);
        return ResponseEntity.ok(payments.stream().map(PaymentPresenter::present).toList());
    }
}
