package br.com.ofisy.ms_ofisy_billing.application.ports;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface StockClient {

    Optional<StockView> findById(UUID stockId);

    record StockView(UUID id, String productName, BigDecimal unitPrice) {}
}
