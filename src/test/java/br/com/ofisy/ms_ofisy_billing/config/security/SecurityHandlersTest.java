package br.com.ofisy.ms_ofisy_billing.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHandlersTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void shouldAnswer401WithJsonBodyWhenNotAuthenticated() throws IOException {
        new OfisyAuthenticationEntryPoint().commence(request, response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"status\": 401");
        assertThat(body).contains("Usuário não autorizado");
    }

    @Test
    void shouldAnswer403WithJsonBodyWhenAccessIsDenied() throws IOException {
        new OfisyAccessDeniedHandler().handle(request, response, new AccessDeniedException("x"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("\"status\": 403");
        assertThat(body).contains("Usuário sem permissão");
    }
}
