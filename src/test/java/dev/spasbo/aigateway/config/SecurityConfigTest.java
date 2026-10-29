package dev.spasbo.aigateway.config;

import dev.spasbo.aigateway.identity.CurrentUser;
import dev.spasbo.aigateway.identity.IdentityService;
import dev.spasbo.aigateway.identity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    IdentityService identityService;

    @BeforeEach
    void resolveEveryCaller() {
        when(identityService.resolve(any()))
                .thenReturn(new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "sub-1", Role.USER));
    }

    @Test
    void apiWithoutTokenIsUnauthorized() {
        var result = mvc.post().uri("/api/chat").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Bearer");
    }

    @Test
    void userRolePassesAuthorization() {
        // No controller in this slice, so getting past security shows up as 404, not 401/403.
        var result = mvc.post().uri("/api/chat").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminRolePassesAuthorization() {
        var result = mvc.post().uri("/api/chat").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void tokenWithoutGatewayRoleIsForbidden() {
        var result = mvc.post().uri("/api/chat").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_profile"))).exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void anythingOutsideApiIsDenied() {
        var result = mvc.get().uri("/internal").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
