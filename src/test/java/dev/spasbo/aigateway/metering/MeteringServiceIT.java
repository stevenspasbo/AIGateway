package dev.spasbo.aigateway.metering;

import dev.spasbo.aigateway.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, MeteringService.class})
class MeteringServiceIT {

    @Autowired
    MeteringService metering;

    @Autowired
    JdbcClient jdbc;

    UUID tenantId;
    UUID userId;

    @BeforeEach
    void seed() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jdbc.sql("insert into tenant (id, name, monthly_token_budget) values (?, 'Agency A', 1000)").param(tenantId).update();
        jdbc.sql("insert into app_user (id, tenant_id, external_subject, role) values (?, ?, 'sub-1', 'USER')")
                .params(userId, tenantId).update();
    }

    @Test
    void writesOneUsageRecordWithProviderCounts() {
        metering.record(new ModelCall(tenantId, userId, "llama3.2:1b", 12, 5, 420, CallStatus.SUCCESS));

        var rows = jdbc.sql("select * from usage_record where tenant_id = ?").param(tenantId).query().listOfRows();
        assertThat(rows).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("user_id", userId).containsEntry("model_id", "llama3.2:1b")
                .containsEntry("input_tokens", 12).containsEntry("output_tokens", 5)
                .containsEntry("latency_ms", 420L).containsEntry("status", "SUCCESS"));
    }

    @Test
    void writesOneAuditEventWithoutPromptText() {
        metering.record(new ModelCall(tenantId, userId, "llama3.2:1b", 12, 5, 420, CallStatus.ERROR));

        var rows = jdbc.sql("select * from audit_event where tenant_id = ?").param(tenantId).query().listOfRows();
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("user_id", userId).containsEntry("action", "CHAT")
                    .containsEntry("model_id", "llama3.2:1b").containsEntry("outcome", "ERROR");
            assertThat(row.get("prompt_text")).isNull();
        });
    }
}
