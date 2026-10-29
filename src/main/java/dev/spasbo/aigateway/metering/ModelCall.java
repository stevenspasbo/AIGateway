package dev.spasbo.aigateway.metering;

import java.util.UUID;

/** One model call as metered: who, which model, provider-reported tokens, wall-clock latency, outcome. */
public record ModelCall(UUID tenantId, UUID userId, String modelId,
                        int inputTokens, int outputTokens, long latencyMs, CallStatus status) {
}
