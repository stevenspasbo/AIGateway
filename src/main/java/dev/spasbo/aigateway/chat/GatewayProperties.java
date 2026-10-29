package dev.spasbo.aigateway.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("gateway")
public record GatewayProperties(@NotEmpty List<@Valid ModelDefinition> models) {
}
