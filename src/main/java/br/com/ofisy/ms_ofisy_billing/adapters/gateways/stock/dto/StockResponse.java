package br.com.ofisy.ms_ofisy_billing.adapters.gateways.stock.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockResponse(UUID id, String productName, BigDecimal unitPrice) {}
