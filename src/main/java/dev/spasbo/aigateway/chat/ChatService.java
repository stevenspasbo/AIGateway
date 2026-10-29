package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.identity.CurrentUser;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class ChatService {

    private final ChatModel chatModel;
    private final Clock clock;

    public ChatService(ChatModel chatModel, Clock clock) {
        this.chatModel = chatModel;
        this.clock = clock;
    }

    public ChatResult chat(CurrentUser user, ModelDefinition model, String message) {
        Instant start = clock.instant();
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(message), optionsFor(model)));
        long latencyMs = Duration.between(start, clock.instant()).toMillis();

        Usage usage = response.getMetadata().getUsage();
        return new ChatResult(response.getResult().getOutput().getText(), model.id(),
                tokens(usage.getPromptTokens()), tokens(usage.getCompletionTokens()), latencyMs);
    }

    /**
     * Spring AI 2.0 does not merge prompt options with the model's defaults: options on a prompt are used
     * as-is. Starting from the model's own options keeps its defaults (and its provider-specific options
     * type) and changes only the model id.
     */
    private ChatOptions optionsFor(ModelDefinition model) {
        return chatModel.getOptions().mutate().model(model.id()).build();
    }

    /** Providers may omit usage; record zero rather than guessing from text length. */
    private static int tokens(Integer reported) {
        return reported == null ? 0 : reported;
    }
}
