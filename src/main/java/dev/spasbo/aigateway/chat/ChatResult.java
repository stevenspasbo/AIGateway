package dev.spasbo.aigateway.chat;

/** A completed model call. Token counts are exactly what the provider reported. */
public record ChatResult(String reply, String modelId, int inputTokens, int outputTokens, long latencyMs) {
}
