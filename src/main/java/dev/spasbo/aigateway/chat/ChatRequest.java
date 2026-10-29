package dev.spasbo.aigateway.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/chat. Tenant and user are deliberately absent: they come from the token. */
public record ChatRequest(
        @NotBlank String model,
        @NotBlank @Size(max = ChatController.MAX_MESSAGE_CHARS) String message) {
}
