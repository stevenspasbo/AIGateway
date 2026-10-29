package dev.spasbo.aigateway.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRolesConverterTest {

    final JwtRolesConverter converter = new JwtRolesConverter();

    @Test
    void mapsGatewayRolesToAuthorities() {
        var auth = converter.convert(jwt(List.of("user", "admin")));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void ignoresRolesTheGatewayDoesNotKnow() {
        var auth = converter.convert(jwt(List.of("offline_access", "default-roles-ai-gateway", "user")));

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
    }

    @Test
    void missingRolesClaimGivesNoAuthorities() {
        var auth = converter.convert(Jwt.withTokenValue("t").header("alg", "none").subject("sub-1").build());

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void principalNameIsTheSubject() {
        var auth = converter.convert(jwt(List.of("user")));

        assertThat(auth.getName()).isEqualTo("sub-1");
    }

    @Test
    void highestRolePrefersAdmin() {
        assertThat(Role.highestOf(List.of("user", "admin"))).contains(Role.ADMIN);
        assertThat(Role.highestOf(List.of("user"))).contains(Role.USER);
        assertThat(Role.highestOf(List.of("offline_access"))).isEmpty();
    }

    private static Jwt jwt(List<String> roles) {
        return Jwt.withTokenValue("t").header("alg", "none").subject("sub-1").claim("roles", roles).build();
    }
}
