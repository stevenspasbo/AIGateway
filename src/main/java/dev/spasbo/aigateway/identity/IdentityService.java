package dev.spasbo.aigateway.identity;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IdentityService {

    public static final String TENANT_CLAIM = "tenant_id";

    private final TenantRepository tenants;

    public IdentityService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public CurrentUser resolve(Jwt jwt) {
        UUID tenantId = tenantIdOf(jwt);
        if (!tenants.existsById(tenantId)) {
            throw new TenantAccessDeniedException("tenant " + tenantId + " not found");
        }
        // Authorization runs before this, so a gateway role is always present here
        Role role = Role.highestOf(JwtRolesConverter.rolesOf(jwt)).orElseThrow();
        return new CurrentUser(tenantId, null, jwt.getSubject(), role);
    }

    private static UUID tenantIdOf(Jwt jwt) {
        String claim = jwt.getClaimAsString(TENANT_CLAIM);
        if (claim == null) {
            throw new TenantAccessDeniedException("tenant_id claim missing");
        }
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException e) {
            throw new TenantAccessDeniedException("tenant_id claim is not a UUID");
        }
    }
}
