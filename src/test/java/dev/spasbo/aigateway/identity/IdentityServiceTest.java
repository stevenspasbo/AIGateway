package dev.spasbo.aigateway.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityServiceTest {

    static final UUID TENANT = UUID.fromString("7f3c1e2a-5b8d-4c6e-9a0f-3d2b1c4e5f60");

    final TenantRepository tenants = mock(TenantRepository.class);
    final AppUserRepository users = mock(AppUserRepository.class);
    final IdentityService identity = new IdentityService(tenants, users);

    @Test
    void knownTenantResolvesToTheUpsertedUser() {
        UUID userId = UUID.randomUUID();
        when(tenants.existsById(TENANT)).thenReturn(true);
        when(users.upsert(TENANT, "sub-1", Role.USER)).thenReturn(userId);

        CurrentUser user = identity.resolve(jwt(TENANT.toString()));

        assertThat(user).isEqualTo(new CurrentUser(TENANT, userId, "sub-1", Role.USER));
    }

    @Test
    void unknownTenantNeverCreatesAUser() {
        when(tenants.existsById(TENANT)).thenReturn(false);

        assertThatThrownBy(() -> identity.resolve(jwt(TENANT.toString())));

        verifyNoInteractions(users);
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
