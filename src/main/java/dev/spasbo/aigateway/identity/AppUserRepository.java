package dev.spasbo.aigateway.identity;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AppUserRepository {

    private final JdbcClient jdbc;

    public AppUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates the user on first sight and refreshes the role from the token on every later request.
     * One atomic statement, so two concurrent first requests cannot create duplicates.
     */
    public UUID upsert(UUID tenantId, String externalSubject, Role role) {
        return jdbc.sql("""
                        insert into app_user (id, tenant_id, external_subject, role)
                        values (:id, :tenantId, :subject, :role)
                        on conflict (tenant_id, external_subject)
                        do update set role = excluded.role, updated_at = now()
                        returning id""")
                .param("id", UUID.randomUUID())
                .param("tenantId", tenantId)
                .param("subject", externalSubject)
                .param("role", role.name())
                .query(UUID.class)
                .single();
    }
}
