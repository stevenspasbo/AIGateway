package dev.spasbo.aigateway.chat;

import jakarta.validation.constraints.NotBlank;

/** One allowlisted model: the id callers send, and which provider adapter serves it. */
public record ModelDefinition(@NotBlank String id, @NotBlank String provider) {
}
