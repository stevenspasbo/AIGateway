package dev.spasbo.aigateway.identity;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantResolutionFilterTest {

    final IdentityService identity = mock(IdentityService.class);
    final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    final FilterChain chain = mock(FilterChain.class);
    final TenantResolutionFilter filter = new TenantResolutionFilter(identity, resolver);

    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/chat");
    final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvedUserIsExposedToTheRequest() throws Exception {
        var jwt = IdentityServiceTest.jwt(UUID.randomUUID().toString());
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var current = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "sub-1", Role.USER);
        when(identity.resolve(jwt)).thenReturn(current);

        filter.doFilter(request, response, chain);

        assertThat(CurrentUser.from(request)).isEqualTo(current);
        verify(chain).doFilter(request, response);
    }

    @Test
    void deniedTenantIsHandedToTheExceptionResolverAndStopsTheChain() throws Exception {
        var jwt = IdentityServiceTest.jwt("bad");
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        var denied = new TenantAccessDeniedException("tenant_id claim is not a UUID");
        when(identity.resolve(jwt)).thenThrow(denied);

        filter.doFilter(request, response, chain);

        verify(resolver).resolveException(request, response, null, denied);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void nonJwtRequestsPassThroughUntouched() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("x", "y"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(identity, never()).resolve(any());
        verify(resolver, never()).resolveException(any(), any(), isNull(), any());
    }
}
