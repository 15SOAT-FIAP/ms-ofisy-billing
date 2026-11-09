package br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.payment.dto.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "API de Cobranças")
public interface PaymentApi {

    @Operation(summary = "Buscar as cobranças de uma ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Cobranças listadas com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<List<PaymentResponseDTO>> findByServiceOrderId(UUID serviceOrderId);
}
