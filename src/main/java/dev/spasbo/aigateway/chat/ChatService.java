package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.identity.CurrentUser;
import dev.spasbo.aigateway.metering.CallStatus;
import dev.spasbo.aigateway.metering.MeteringService;
import dev.spasbo.aigateway.metering.ModelCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class ChatService {

    private final ChatModel chatModel;
    private final MeteringService metering;
    private final Clock clock;

    public ChatService(ChatModel chatModel, MeteringService metering, Clock clock) {
        this.chatModel = chatModel;
        this.metering = metering;
        this.clock = clock;
    }

    /** Every call is metered exactly once, whether it succeeds or fails. */
    public ChatResult chat(CurrentUser user, ModelDefinition model, String message) {
        Instant start = clock.instant();
        ChatResponse response;
        try {
            response = chatModel.call(new Prompt(new UserMessage(message), optionsFor(model)));
        } catch (RuntimeException e) {
            meter(user, model, 0, 0, elapsedMs(start), CallStatus.ERROR);
            throw new ModelCallFailedException(model.id(), e);
        }
        long latencyMs = elapsedMs(start);

        Generation generation = response.getResult();
        if (generation == null) {
            meter(user, model, 0, 0, latencyMs, CallStatus.ERROR);
            throw new ModelCallFailedException(model.id(), "provider returned no generation");
        }

        Usage usage = response.getMetadata().getUsage();
        var result = new ChatResult(generation.getOutput().getText(), model.id(),
                tokens(usage.getPromptTokens()), tokens(usage.getCompletionTokens()), latencyMs);
        meter(user, model, result.inputTokens(), result.outputTokens(), latencyMs, CallStatus.SUCCESS);
        return result;
    }

    /**
     * Spring AI 2.0 does not merge prompt options with the model's defaults: options on a prompt are used
     * as-is. Starting from the model's own options keeps its defaults (and its provider-specific options
     * type) and changes only the model id.
     */
    private ChatOptions optionsFor(ModelDefinition model) {
        return chatModel.getOptions().mutate().model(model.id()).build();
    }

    private void meter(CurrentUser user, ModelDefinition model, int in, int out, long latencyMs, CallStatus status) {
        metering.record(new ModelCall(user.tenantId(), user.userId(), model.id(), in, out, latencyMs, status));
    }

    private long elapsedMs(Instant start) {
        return Duration.between(start, clock.instant()).toMillis();
    }

    /** Providers may omit usage; record zero rather than guessing from text length. */
    private static int tokens(Integer reported) {
        return reported == null ? 0 : reported;
    }
}
