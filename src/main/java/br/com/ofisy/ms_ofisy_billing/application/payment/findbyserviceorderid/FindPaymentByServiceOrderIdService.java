package br.com.ofisy.ms_ofisy_billing.application.payment.findbyserviceorderid;

import br.com.ofisy.ms_ofisy_billing.domain.payment.Payment;
import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FindPaymentByServiceOrderIdService implements FindPaymentByServiceOrderIdUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Payment> execute(UUID serviceOrderId) {
        return paymentRepository.findByServiceOrderId(serviceOrderId);
    }
}
