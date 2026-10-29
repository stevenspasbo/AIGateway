package dev.spasbo.aigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void apiWithoutTokenIsUnauthorized() {
        var result = mvc.post().uri("/api/chat").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Bearer");
    }

    @Test
    void apiWithTokenPassesAuthentication() {
        // No controller in this slice, so getting past security shows up as 404, not 401/403.
        var result = mvc.post().uri("/api/chat").with(jwt()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void anythingOutsideApiIsDenied() {
        var result = mvc.get().uri("/internal").with(jwt()).exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
