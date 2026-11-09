package br.com.ofisy.ms_ofisy_billing.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldLetAccessDeniedPassSoTheSecurityChainAnswers403() {
        AccessDeniedException exception = new AccessDeniedException("sem permissão");

        assertThatThrownBy(() -> handler.rethrowSecurityException(exception)).isSameAs(exception);
    }

    @Test
    void shouldLetAuthenticationFailuresPassSoTheSecurityChainAnswers401() {
        BadCredentialsException exception = new BadCredentialsException("inválido");

        assertThatThrownBy(() -> handler.rethrowSecurityException(exception)).isSameAs(exception);
    }
}