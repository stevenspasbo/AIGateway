package dev.spasbo.aigateway.identity;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Objects;

/** Maps the Keycloak {@code roles} claim to ROLE_ authorities, keeping only roles the gateway defines. */
public class JwtRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String ROLES_CLAIM = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var authorities = Role.fromClaims(rolesOf(jwt))
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    static List<String> rolesOf(Jwt jwt) {
        return Objects.requireNonNullElse(jwt.getClaimAsStringList(ROLES_CLAIM), List.of());
    }
}
