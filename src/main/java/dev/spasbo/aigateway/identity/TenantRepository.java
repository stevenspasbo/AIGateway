package dev.spasbo.aigateway.identity;

import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface TenantRepository extends Repository<Tenant, UUID> {

    boolean existsById(UUID id);
}
