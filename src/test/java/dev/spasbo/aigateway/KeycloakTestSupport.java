package dev.spasbo.aigateway;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;

/**
 * One Keycloak container per test JVM, importing the same realm export as docker-compose.yml.
 * Realm import is slow, so the container is started once and never stopped (Ryuk cleans up).
 */
public final class KeycloakTestSupport {

    public static final String REALM = "ai-gateway";
    public static final String CLIENT_ID = "ai-gateway-cli";

    private static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer("quay.io/keycloak/keycloak:26.8.1").withRealmImportFile("/ai-gateway-realm.json");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private KeycloakTestSupport() {
    }

    public static synchronized String issuerUri() {
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
        }
        return KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;
    }

    /** Password grant for a dev user from the realm export; the password convention is {@code <username>-dev-only}. */
    public static String accessToken(String username) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("client_id", CLIENT_ID);
        form.add("username", username);
        form.add("password", username + "-dev-only");

        JsonNode body = RestClient.create().post()
                .uri(issuerUri() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        return body.get("access_token").asString();
    }

    /** Decodes the payload without verifying the signature; for asserting on claims only. */
    public static JsonNode claims(String accessToken) {
        String payload = accessToken.split("\\.")[1];
        return JSON.readTree(Base64.getUrlDecoder().decode(payload));
    }
}
