package dev.spasbo.aigateway.metering;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes the usage record and the audit event for a model call together. Called after the model
 * returns, so the short transaction never holds a database connection open during a slow model call.
 */
@Service
public class MeteringService {

    static final String CHAT_ACTION = "CHAT";

    private final JdbcClient jdbc;

    public MeteringService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void record(ModelCall call) {
        jdbc.sql("""
                        insert into usage_record
                            (id, tenant_id, user_id, model_id, input_tokens, output_tokens, latency_ms, status)
                        values (?, ?, ?, ?, ?, ?, ?, ?)""")
                .params(UUID.randomUUID(), call.tenantId(), call.userId(), call.modelId(),
                        call.inputTokens(), call.outputTokens(), call.latencyMs(), call.status().name())
                .update();
        // prompt_text stays null until F8 adds the opt-in audit flag
        jdbc.sql("""
                        insert into audit_event (id, tenant_id, user_id, action, model_id, outcome)
                        values (?, ?, ?, ?, ?, ?)""")
                .params(UUID.randomUUID(), call.tenantId(), call.userId(), CHAT_ACTION, call.modelId(),
                        call.status().name())
                .update();
    }
}
