package br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment.dto;

import br.com.ofisy.ms_ofisy_billing.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDTO(
        UUID id,
        UUID serviceOrderId,
        UUID quoteId,
        BigDecimal amount,
        PaymentStatus status,
        String checkoutUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
