package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote;

import br.com.ofisy.ms_ofisy_billing.application.quote.approve.ApproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.QuoteNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockNotFoundException;
import br.com.ofisy.ms_ofisy_billing.application.quote.exceptions.StockUnavailableException;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyid.FindQuoteByIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyserviceorderid.FindQuoteByServiceOrderIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.reprove.ReproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.update.UpdateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CreateQuoteUseCase createQuoteUseCase;
    @MockitoBean
    private UpdateQuoteUseCase updateQuoteUseCase;
    @MockitoBean
    private ApproveQuoteUseCase approveQuoteUseCase;
    @MockitoBean
    private ReproveQuoteUseCase reproveQuoteUseCase;
    @MockitoBean
    private FindQuoteByIdUseCase findQuoteByIdUseCase;
    @MockitoBean
    private FindQuoteByServiceOrderIdUseCase findQuoteByServiceOrderIdUseCase;

    @Test
    @WithMockUser(roles = "ATTENDANT")
    void shouldCreateQuote() throws Exception {
        Quote quote = sampleQuote();
        when(createQuoteUseCase.execute(any())).thenReturn(quote);

        String body = """
                {"serviceOrderId": "%s", "stockItems": [], "serviceItems": []}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/quotes").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ATTENDANT")
    void shouldCreateQuoteWithOnlyTheStockIdAndTheQuantityOfEachStockItem() throws Exception {
        UUID serviceOrderId = UUID.randomUUID();
        UUID stockId = UUID.randomUUID();
        when(createQuoteUseCase.execute(any())).thenReturn(sampleQuote());

        String body = """
                {"serviceOrderId": "%s", "stockItems": [{"stockId": "%s", "quantity": 2}], "serviceItems": []}
                """.formatted(serviceOrderId, stockId);

        mockMvc.perform(post("/api/v1/quotes").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateQuoteUseCase.CreateQuoteCommand> captor =
                ArgumentCaptor.forClass(CreateQuoteUseCase.CreateQuoteCommand.class);
        verify(createQuoteUseCase).execute(captor.capture());
        assertThat(captor.getValue().stockItems())
                .containsExactly(new CreateQuoteUseCase.StockItemCommand(stockId, 2));
    }

    @Test
    @WithMockUser(roles = "ATTENDANT")
    void shouldReturn404WhenTheStockItemDoesNotExist() throws Exception {
        UUID stockId = UUID.randomUUID();
        when(createQuoteUseCase.execute(any())).thenThrow(new StockNotFoundException(stockId));

        String body = """
                {"serviceOrderId": "%s", "stockItems": [{"stockId": "%s", "quantity": 1}], "serviceItems": []}
                """.formatted(UUID.randomUUID(), stockId);

        mockMvc.perform(post("/api/v1/quotes").contentType("application/json").content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ATTENDANT")
    void shouldReturn503WhenTheStockIsUnavailable() throws Exception {
        UUID stockId = UUID.randomUUID();
        when(createQuoteUseCase.execute(any()))
                .thenThrow(new StockUnavailableException(stockId, new RuntimeException("timeout")));

        String body = """
                {"serviceOrderId": "%s", "stockItems": [{"stockId": "%s", "quantity": 1}], "serviceItems": []}
                """.formatted(UUID.randomUUID(), stockId);

        mockMvc.perform(post("/api/v1/quotes").contentType("application/json").content(body))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldApproveQuoteWithoutAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        Quote quote = sampleQuote();
        quote.sendForApproval();
        quote.approve();
        when(approveQuoteUseCase.execute(id)).thenReturn(quote);

        mockMvc.perform(patch("/api/v1/quotes/{id}/approve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldReturn404WhenApprovingAMissingQuote() throws Exception {
        UUID id = UUID.randomUUID();
        when(approveQuoteUseCase.execute(id)).thenThrow(new QuoteNotFoundException(id));

        mockMvc.perform(patch("/api/v1/quotes/{id}/approve", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReproveQuoteWithAReason() throws Exception {
        UUID id = UUID.randomUUID();
        Quote quote = sampleQuote();
        quote.sendForApproval();
        quote.reprove("Preço alto");
        when(reproveQuoteUseCase.execute(any())).thenReturn(quote);

        mockMvc.perform(patch("/api/v1/quotes/{id}/reprove", id)
                        .contentType("application/json")
                        .content("{\"reason\": \"Preço alto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteRefusalReason").value("Preço alto"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFindQuoteById() throws Exception {
        UUID id = UUID.randomUUID();
        when(findQuoteByIdUseCase.execute(id)).thenReturn(sampleQuote());

        mockMvc.perform(get("/api/v1/quotes/{id}", id))
                .andExpect(status().isOk());
    }

    private Quote sampleQuote() {
        return Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
    }
}
