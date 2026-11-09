package br.com.ofisy.ms_ofisy_billing.adapters.controllers.quote;

import br.com.ofisy.ms_ofisy_billing.application.quote.approve.ApproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.create.CreateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyid.FindQuoteByIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.findbyserviceorderid.FindQuoteByServiceOrderIdUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.reprove.ReproveQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.application.quote.update.UpdateQuoteUseCase;
import br.com.ofisy.ms_ofisy_billing.config.jwt.JwtProperties;
import br.com.ofisy.ms_ofisy_billing.config.security.OfisyAccessDeniedHandler;
import br.com.ofisy.ms_ofisy_billing.config.security.OfisyAuthenticationEntryPoint;
import br.com.ofisy.ms_ofisy_billing.config.security.SecurityConfig;
import br.com.ofisy.ms_ofisy_billing.config.security.SecurityProperties;
import br.com.ofisy.ms_ofisy_billing.domain.quote.Quote;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteServiceItem;
import br.com.ofisy.ms_ofisy_billing.domain.quote.QuoteStockItem;
import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtService;
import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtTestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
@Import({SecurityConfig.class, JwtService.class,
        OfisyAccessDeniedHandler.class, OfisyAuthenticationEntryPoint.class})
@TestPropertySource(properties = "jwt.secret=" + JwtTestTokens.SECRET)
class QuoteControllerSecurityTest {

    @TestConfiguration
    @EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
    static class PropertiesConfig {
    }

    private static final String QUOTE_URL = "/api/v1/quotes/{id}";

    @Autowired
    private MockMvc mockMvc;

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

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void stubUseCases() {
        Quote quote = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
        when(findQuoteByIdUseCase.execute(id)).thenReturn(quote);
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(QUOTE_URL, id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Usuário não autorizado"));
    }

    @Test
    void shouldReturn401ForTokenSignedWithAnotherSecret() throws Exception {
        String token = JwtTestTokens.withRole(JwtTestTokens.OTHER_SECRET, "ADMIN");

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForExpiredToken() throws Exception {
        String token = JwtTestTokens.expired(JwtTestTokens.SECRET, "ADMIN");

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForCustomerToken() throws Exception {
        String token = JwtTestTokens.withIssuer(JwtTestTokens.SECRET, JwtTestTokens.CUSTOMER_ISSUER, "ADMIN");

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForRoleWithoutPermission() throws Exception {
        String token = JwtTestTokens.withRole(JwtTestTokens.SECRET, "STOCKMAN");

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Usuário sem permissão"));
    }

    @Test
    void shouldReturn403ForTokenWithoutRole() throws Exception {
        String token = JwtTestTokens.withoutRole(JwtTestTokens.SECRET);

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATTENDANT", "MECHANIC"})
    void shouldReturn200ForStaffRoles(String role) throws Exception {
        String token = JwtTestTokens.withRole(JwtTestTokens.SECRET, role);

        mockMvc.perform(get(QUOTE_URL, id).header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldLetCustomersApproveWithoutToken() throws Exception {
        Quote approved = Quote.create(UUID.randomUUID(),
                List.of(QuoteStockItem.create(UUID.randomUUID(), "Filtro de óleo", BigDecimal.TEN, 1)),
                List.of(QuoteServiceItem.create(UUID.randomUUID(), BigDecimal.TEN)));
        approved.sendForApproval();
        approved.approve();
        when(approveQuoteUseCase.execute(id)).thenReturn(approved);

        mockMvc.perform(patch("/api/v1/quotes/{id}/approve", id))
                .andExpect(status().isOk());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
