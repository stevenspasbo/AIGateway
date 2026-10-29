package dev.spasbo.aigateway.identity;

import dev.spasbo.aigateway.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local")
class LocalProfileSeedIT {

    static final UUID LOCAL_TENANT_ID = UUID.fromString("7f3c1e2a-5b8d-4c6e-9a0f-3d2b1c4e5f60");

    @Autowired
    JdbcClient jdbc;

    @Test
    void localProfileSeedsTheDevTenant() {
        var row = jdbc.sql("select name, monthly_token_budget from tenant where id = ?")
                .param(LOCAL_TENANT_ID).query().singleRow();

        assertThat(row).containsEntry("name", "Local Dev Agency")
                .containsEntry("monthly_token_budget", 1_000_000L);
    }
}
