package dev.spasbo.aigateway.config;

import dev.spasbo.aigateway.web.ApiSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(SecurityConfigTest.ProbeController.class)
@Import(SecurityConfigTest.ProbeController.class)
class SecurityConfigTest extends ApiSliceTest {

    @RestController
    static class ProbeController {
        @GetMapping({"/api/probe", "/internal"})
        String probe() {
            return "ok";
        }
    }

    @Test
    void apiWithoutTokenIsUnauthorized() {
        var result = mvc.get().uri("/api/probe").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).startsWith("Bearer");
    }

    @Test
    void userRolePassesAuthorization() {
        assertThat(mvc.get().uri("/api/probe").with(user()).exchange()).hasStatus(HttpStatus.OK);
    }

    @Test
    void adminRolePassesAuthorization() {
        var result = mvc.get().uri("/api/probe").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
    }

    @Test
    void tokenWithoutGatewayRoleIsForbidden() {
        var result = mvc.get().uri("/api/probe").with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_profile"))).exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void anythingOutsideApiIsDenied() {
        var result = mvc.get().uri("/internal").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))).exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
    }
}
