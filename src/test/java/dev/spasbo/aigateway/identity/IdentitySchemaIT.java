package dev.spasbo.aigateway.identity;

import dev.spasbo.aigateway.TestcontainersConfiguration;
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
class IdentitySchemaIT {

    @Autowired
    JdbcClient jdbc;

    @Test
    void appUserBelongsToExistingTenant() {
        UUID tenantId = insertTenant("Agency A");

        insertUser(tenantId, "sub-1", "USER");

        Integer count = jdbc.sql("select count(*) from app_user where tenant_id = ?")
                .param(tenantId).query(Integer.class).single();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void appUserForUnknownTenantIsRejected() {
        assertThatThrownBy(() -> insertUser(UUID.randomUUID(), "sub-1", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void subjectIsUniqueWithinTenant() {
        UUID tenantId = insertTenant("Agency A");
        insertUser(tenantId, "sub-1", "USER");

        assertThatThrownBy(() -> insertUser(tenantId, "sub-1", "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleMustBeUserOrAdmin() {
        UUID tenantId = insertTenant("Agency A");

        assertThatThrownBy(() -> insertUser(tenantId, "sub-1", "ROOT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tenantBudgetCannotBeNegative() {
        assertThatThrownBy(() -> jdbc.sql("insert into tenant (id, name, monthly_token_budget) values (?, ?, -1)")
                .params(UUID.randomUUID(), "Bad").update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertTenant(String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into tenant (id, name, monthly_token_budget) values (?, ?, ?)")
                .params(id, name, 1_000_000L).update();
        return id;
    }

    private void insertUser(UUID tenantId, String subject, String role) {
        jdbc.sql("insert into app_user (id, tenant_id, external_subject, role) values (?, ?, ?, ?)")
                .params(UUID.randomUUID(), tenantId, subject, role).update();
    }
}
