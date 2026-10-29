package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.KeycloakTestSupport;
import dev.spasbo.aigateway.TestcontainersConfiguration;
import dev.spasbo.aigateway.testsupport.FakeChatModel;
import dev.spasbo.aigateway.testsupport.FakeChatModelConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import static dev.spasbo.aigateway.KeycloakTestSupport.accessToken;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * F1 end to end: real Keycloak token, real security chain, real Postgres and migrations (local profile,
 * so the dev tenant is seeded), and a scripted fake in place of the model provider.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.ai.model.chat=none")
@Import({TestcontainersConfiguration.class, FakeChatModelConfig.class})
@ActiveProfiles("local")
class ChatEndToEndIT {

    @DynamicPropertySource
    static void keycloak(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", KeycloakTestSupport::issuerUri);
    }

    @LocalServerPort
    int port;

    @Autowired
    FakeChatModel model;

    @Autowired
    JdbcClient jdbc;

    RestClient http;

    @BeforeEach
    void setUp() {
        model.reset();
        jdbc.sql("delete from audit_event").update();
        jdbc.sql("delete from usage_record").update();
        jdbc.sql("delete from app_user").update();
        http = RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> { })
                .build();
    }

    @Test
    void chatReturnsTheReplyAndWritesOneUsageRecordAndOneAuditEvent() {
        model.reply("Hello, Alice", 12, 5);

        ResponseEntity<JsonNode> response = chat("alice", "llama3.2:1b", "Hi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("reply").asString()).isEqualTo("Hello, Alice");
        assertThat(response.getBody().get("inputTokens").asInt()).isEqualTo(12);

        var usage = jdbc.sql("""
                select u.status, u.input_tokens, u.output_tokens, u.model_id, a.external_subject
                from usage_record u join app_user a on a.id = u.user_id""").query().listOfRows();
        assertThat(usage).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("status", "SUCCESS").containsEntry("input_tokens", 12)
                .containsEntry("output_tokens", 5).containsEntry("model_id", "llama3.2:1b"));
        assertThat(count("audit_event where outcome = 'SUCCESS' and action = 'CHAT'")).isEqualTo(1);
    }

    @Test
    void firstRequestCreatesTheAppUserWithRoleFromTheToken() {
        model.reply("ok", 1, 1).reply("ok", 1, 1);

        chat("ada-admin", "llama3.2:1b", "Hi");
        chat("ada-admin", "llama3.2:1b", "Again");

        assertThat(jdbc.sql("select role from app_user").query(String.class).list()).containsExactly("ADMIN");
        assertThat(count("usage_record")).isEqualTo(2);
    }

    @Test
    void failedModelCallReturns502AndStillWritesBothRecords() {
        model.fail(new ResourceAccessException("Connection refused"));

        ResponseEntity<JsonNode> response = chat("alice", "llama3.2:1b", "Hi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().toString()).doesNotContain("Connection refused").doesNotContain("Exception");
        assertThat(count("usage_record where status = 'ERROR' and input_tokens = 0 and output_tokens = 0")).isEqualTo(1);
        assertThat(count("audit_event where outcome = 'ERROR'")).isEqualTo(1);
    }

    @Test
    void modelOutsideTheAllowlistIs400AndCallsNoModel() {
        ResponseEntity<JsonNode> response = chat("alice", "gpt-4o", "Hi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(model.prompts()).isEmpty();
        assertThat(count("usage_record")).isZero();
    }

    @Test
    void tenantNotInTheDatabaseIs403ProblemDetail() {
        ResponseEntity<JsonNode> response = chat("orphan", "llama3.2:1b", "Hi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("title").asString()).isEqualTo("Tenant not recognized");
        assertThat(model.prompts()).isEmpty();
        assertThat(count("app_user")).isZero();
    }

    @Test
    void requestWithoutTokenIs401() {
        var response = http.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .body("{\"model\": \"llama3.2:1b\", \"message\": \"Hi\"}").retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void modelsEndpointListsTheAllowlist() {
        var response = http.get().uri("/api/models").header("Authorization", "Bearer " + accessToken("alice"))
                .retrieve().toEntity(JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).get("id").asString()).isEqualTo("llama3.2:1b");
    }

    private ResponseEntity<JsonNode> chat(String username, String modelId, String message) {
        return http.post().uri("/api/chat")
                .header("Authorization", "Bearer " + accessToken(username))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"model\": \"" + modelId + "\", \"message\": \"" + message + "\"}")
                .retrieve().toEntity(JsonNode.class);
    }

    private int count(String fromWhere) {
        return jdbc.sql("select count(*) from " + fromWhere).query(Integer.class).single();
    }
}
