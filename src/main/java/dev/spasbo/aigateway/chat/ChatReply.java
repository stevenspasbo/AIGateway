package dev.spasbo.aigateway.chat;

public record ChatReply(String reply, String model, int inputTokens, int outputTokens, long latencyMs) {

    static ChatReply from(ChatResult result) {
        return new ChatReply(result.reply(), result.modelId(), result.inputTokens(), result.outputTokens(),
                result.latencyMs());
    }
}
