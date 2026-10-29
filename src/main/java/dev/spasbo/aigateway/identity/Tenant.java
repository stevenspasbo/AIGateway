package dev.spasbo.aigateway.identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("tenant")
public record Tenant(@Id UUID id, String name, long monthlyTokenBudget) {
}
