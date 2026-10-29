package dev.spasbo.aigateway.identity;

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
@Import({TestcontainersConfiguration.class, AppUserRepository.class})
class AppUserRepositoryIT {

    @Autowired
    AppUserRepository users;

    @Autowired
    JdbcClient jdbc;

    UUID tenantA;
    UUID tenantB;

    @BeforeEach
    void seed() {
        tenantA = insertTenant("Agency A");
        tenantB = insertTenant("Agency B");
    }

    @Test
    void firstRequestCreatesTheUser() {
        UUID id = users.upsert(tenantA, "sub-1", Role.USER);

        assertThat(roleOf(id)).isEqualTo("USER");
    }

    @Test
    void laterRequestsKeepTheIdAndRefreshTheRole() {
        UUID first = users.upsert(tenantA, "sub-1", Role.USER);

        UUID second = users.upsert(tenantA, "sub-1", Role.ADMIN);

        assertThat(second).isEqualTo(first);
        assertThat(roleOf(first)).isEqualTo("ADMIN");
        assertThat(jdbc.sql("select count(*) from app_user").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void sameSubjectInAnotherTenantIsADifferentUser() {
        UUID inA = users.upsert(tenantA, "sub-1", Role.USER);

        UUID inB = users.upsert(tenantB, "sub-1", Role.USER);

        assertThat(inB).isNotEqualTo(inA);
    }

    private String roleOf(UUID id) {
        return jdbc.sql("select role from app_user where id = ?").param(id).query(String.class).single();
    }

    private UUID insertTenant(String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into tenant (id, name, monthly_token_budget) values (?, ?, 1000)").params(id, name).update();
        return id;
    }
}
