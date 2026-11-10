package br.com.ofisy.ms_ofisy_billing.shared.securityfilter;

import br.com.ofisy.ms_ofisy_billing.shared.jwt.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String TOKEN = "some.jwt.token";
    private static final String EMAIL = "mecanico@ofisy.com";

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenThereIsNoAuthorizationHeader() throws ServletException, IOException {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void shouldIgnoreAuthorizationHeaderThatIsNotBearer() throws ServletException, IOException {
        request.addHeader(JwtAuthFilter.AUTHORIZATION, "Basic abc");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws ServletException, IOException {
        request.addHeader(JwtAuthFilter.AUTHORIZATION, JwtAuthFilter.BEARER + TOKEN);
        when(jwtService.isValidToken(TOKEN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldAuthenticateWithTheRoleFromTheToken() throws ServletException, IOException {
        request.addHeader(JwtAuthFilter.AUTHORIZATION, JwtAuthFilter.BEARER + TOKEN);
        when(jwtService.isValidToken(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(jwtService.extractRole(TOKEN)).thenReturn("MECHANIC");

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_MECHANIC");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldAuthenticateWithoutAuthoritiesWhenTokenHasNoRole() throws ServletException, IOException {
        request.addHeader(JwtAuthFilter.AUTHORIZATION, JwtAuthFilter.BEARER + TOKEN);
        when(jwtService.isValidToken(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
        when(jwtService.extractRole(TOKEN)).thenReturn(null);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(EMAIL);
        assertThat(authentication.getAuthorities()).isEmpty();
    }
}
