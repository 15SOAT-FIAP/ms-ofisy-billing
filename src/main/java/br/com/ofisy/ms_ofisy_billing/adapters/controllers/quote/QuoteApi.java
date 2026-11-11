package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote;

import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.CreateQuoteRequestDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.QuoteResponseDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.ReproveQuoteRequestDTO;
import br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote.dto.UpdateQuoteRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "API de Orçamentos")
public interface QuoteApi {

    @Operation(summary = "Criar o orçamento de uma ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)",
            description = "Informe só o stockId e a quantidade dos itens de estoque: o nome e o preço do produto são lidos do ms-ofisy-stock. "
                    + "Com itens de estoque, o orçamento nasce PENDING e aguarda o estoque dar baixa; só com serviços, nasce AWAITING_APPROVAL.")
    @ApiResponse(responseCode = "201", description = "Orçamento criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @ApiResponse(responseCode = "404", description = "Serviço do catálogo ou item de estoque inexistente")
    @ApiResponse(responseCode = "409", description = "A ordem de serviço já tem orçamento ou há item duplicado")
    @ApiResponse(responseCode = "503", description = "ms-ofisy-stock indisponível ao consultar o item")
    ResponseEntity<QuoteResponseDTO> create(CreateQuoteRequestDTO request);

    @Operation(summary = "Editar o orçamento - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)",
            description = "Só vale para orçamento AWAITING_APPROVAL. Os itens que já estavam no orçamento mantêm o preço e o nome e só mudam a quantidade; "
                    + "itens novos são consultados no estoque. Toda edição publica o QuoteUpdated e devolve o orçamento a PENDING até o estoque confirmar.")
    @ApiResponse(responseCode = "200", description = "Orçamento editado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @ApiResponse(responseCode = "404", description = "Orçamento, serviço do catálogo ou item de estoque inexistente")
    @ApiResponse(responseCode = "409", description = "Item duplicado")
    @ApiResponse(responseCode = "422", description = "O orçamento não está AWAITING_APPROVAL")
    @ApiResponse(responseCode = "503", description = "ms-ofisy-stock indisponível ao consultar o item")
    ResponseEntity<QuoteResponseDTO> update(UUID id, UpdateQuoteRequestDTO request);

    @Operation(summary = "Aprovar o orçamento - Rota pública (protegida pelo Lambda Authorizer no API Gateway)")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Orçamento aprovado com sucesso")
    @ApiResponse(responseCode = "404", description = "Orçamento inexistente")
    @ApiResponse(responseCode = "422", description = "O orçamento não está AWAITING_APPROVAL")
    ResponseEntity<QuoteResponseDTO> approve(UUID id);

    @Operation(summary = "Reprovar o orçamento - Rota pública (protegida pelo Lambda Authorizer no API Gateway)")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Orçamento reprovado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Orçamento inexistente")
    @ApiResponse(responseCode = "422", description = "O orçamento não está AWAITING_APPROVAL")
    ResponseEntity<QuoteResponseDTO> reprove(UUID id, ReproveQuoteRequestDTO request);

    @Operation(summary = "Buscar um orçamento pelo id - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Orçamento encontrado")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    @ApiResponse(responseCode = "404", description = "Orçamento inexistente")
    ResponseEntity<QuoteResponseDTO> findById(UUID id);

    @Operation(summary = "Buscar os orçamentos de uma ordem de serviço - Roles autorizadas: (ADMIN, ATTENDANT, MECHANIC)")
    @ApiResponse(responseCode = "200", description = "Orçamentos listados com sucesso")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "403", description = "Usuário sem permissão")
    ResponseEntity<List<QuoteResponseDTO>> findByServiceOrderId(UUID serviceOrderId);
}
