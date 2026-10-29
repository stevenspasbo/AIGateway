package dev.spasbo.aigateway.identity;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static dev.spasbo.aigateway.KeycloakTestSupport.accessToken;
import static dev.spasbo.aigateway.KeycloakTestSupport.claims;
import static dev.spasbo.aigateway.KeycloakTestSupport.issuerUri;
import static org.assertj.core.api.Assertions.assertThat;

/** Contract test for keycloak/ai-gateway-realm.json: the claims the gateway relies on are really in the token. */
class KeycloakRealmIT {

    static final String LOCAL_TENANT = "7f3c1e2a-5b8d-4c6e-9a0f-3d2b1c4e5f60";

    @Test
    void userTokenCarriesTenantRolesAndAudience() {
        JsonNode claims = claims(accessToken("alice"));

        assertThat(claims.get("iss").asString()).isEqualTo(issuerUri());
        assertThat(claims.get("tenant_id").asString()).isEqualTo(LOCAL_TENANT);
        assertThat(claims.get("roles").valueStream().map(JsonNode::asString)).contains("user").doesNotContain("admin");
        assertThat(audience(claims)).contains("ai-gateway-api");
    }

    @Test
    void adminTokenCarriesAdminRole() {
        JsonNode claims = claims(accessToken("ada-admin"));

        assertThat(claims.get("roles").valueStream().map(JsonNode::asString)).contains("user", "admin");
    }

    @Test
    void orphanUserPointsAtTenantThatIsNotSeeded() {
        JsonNode claims = claims(accessToken("orphan"));

        assertThat(claims.get("tenant_id").asString()).isNotEqualTo(LOCAL_TENANT);
    }

    private static java.util.List<String> audience(JsonNode claims) {
        JsonNode aud = claims.get("aud");
        return aud.isArray() ? aud.valueStream().map(JsonNode::asString).toList() : java.util.List.of(aud.asString());
    }
}
