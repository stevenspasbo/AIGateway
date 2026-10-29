package dev.spasbo.aigateway.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityServiceTest {

    static final UUID TENANT = UUID.fromString("7f3c1e2a-5b8d-4c6e-9a0f-3d2b1c4e5f60");

    final TenantRepository tenants = mock(TenantRepository.class);
    final IdentityService identity = new IdentityService(tenants);

    @Test
    void knownTenantResolves() {
        when(tenants.existsById(TENANT)).thenReturn(true);

        CurrentUser user = identity.resolve(jwt(TENANT.toString()));

        assertThat(user.tenantId()).isEqualTo(TENANT);
        assertThat(user.role()).isEqualTo(Role.USER);
    }

    @Test
    void unknownTenantIsDenied() {
        when(tenants.existsById(TENANT)).thenReturn(false);

        assertThatThrownBy(() -> identity.resolve(jwt(TENANT.toString())))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void malformedTenantIsDenied() {
        assertThatThrownBy(() -> identity.resolve(jwt("agency-a")))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("not a UUID");
    }

    @Test
    void missingTenantIsDenied() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("sub-1").claim("roles", List.of("user")).build();

        assertThatThrownBy(() -> identity.resolve(jwt))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("missing");
    }

    static Jwt jwt(String tenantId) {
        return Jwt.withTokenValue("t").header("alg", "none").subject("sub-1")
                .claim("tenant_id", tenantId).claim("roles", List.of("user")).build();
    }
}
