package dev.spasbo.aigateway.metering;

import dev.spasbo.aigateway.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MeteringSchemaIT {

    @Autowired
    JdbcClient jdbc;

    UUID tenantId;
    UUID userId;

    @BeforeEach
    void seed() {
        tenantId = insertTenant("Agency A");
        userId = insertUser(tenantId);
    }

    @Test
    void usageRecordStoresTokensLatencyAndStatus() {
        insertUsage(tenantId, userId, 12, 34, "SUCCESS");

        var row = jdbc.sql("select input_tokens, output_tokens, latency_ms, status from usage_record where user_id = ?")
                .param(userId).query().singleRow();
        assertThat(row).containsEntry("input_tokens", 12).containsEntry("output_tokens", 34)
                .containsEntry("latency_ms", 250L).containsEntry("status", "SUCCESS");
    }

    @Test
    void usageRecordRejectsNegativeTokens() {
        assertThatThrownBy(() -> insertUsage(tenantId, userId, -1, 0, "SUCCESS"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usageRecordRejectsUnknownStatus() {
        assertThatThrownBy(() -> insertUsage(tenantId, userId, 0, 0, "MAYBE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usageRecordUserMustBelongToSameTenant() {
        UUID otherTenant = insertTenant("Agency B");

        assertThatThrownBy(() -> insertUsage(otherTenant, userId, 0, 0, "SUCCESS"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void auditEventPromptTextIsNullable() {
        jdbc.sql("""
                insert into audit_event (id, tenant_id, user_id, action, model_id, outcome)
                values (?, ?, ?, 'CHAT', 'llama3.2:1b', 'SUCCESS')""")
                .params(UUID.randomUUID(), tenantId, userId).update();

        var row = jdbc.sql("select prompt_text, created_at from audit_event where user_id = ?")
                .param(userId).query().singleRow();
        assertThat(row.get("prompt_text")).isNull();
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void auditEventUserMustBelongToSameTenant() {
        UUID otherTenant = insertTenant("Agency B");

        assertThatThrownBy(() -> jdbc.sql("""
                insert into audit_event (id, tenant_id, user_id, action, model_id, outcome)
                values (?, ?, ?, 'CHAT', 'llama3.2:1b', 'SUCCESS')""")
                .params(UUID.randomUUID(), otherTenant, userId).update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertUsage(UUID tenant, UUID user, int in, int out, String status) {
        jdbc.sql("""
                insert into usage_record (id, tenant_id, user_id, model_id, input_tokens, output_tokens, latency_ms, status)
                values (?, ?, ?, 'llama3.2:1b', ?, ?, 250, ?)""")
                .params(UUID.randomUUID(), tenant, user, in, out, status).update();
    }

    private UUID insertTenant(String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into tenant (id, name, monthly_token_budget) values (?, ?, 1000)").params(id, name).update();
        return id;
    }

    private UUID insertUser(UUID tenant) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into app_user (id, tenant_id, external_subject, role) values (?, ?, ?, 'USER')")
                .params(id, tenant, "sub-" + id).update();
        return id;
    }
}
