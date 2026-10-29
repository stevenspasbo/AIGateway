package dev.spasbo.aigateway.testsupport;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * Scripted stand-in for a real provider. Tests queue replies or failures and inspect the prompts the
 * gateway sent, so they assert on structure (model id, token counts) and never on model wording.
 */
public class FakeChatModel implements ChatModel {

    private final Deque<Supplier<ChatResponse>> script = new ArrayDeque<>();
    private final List<Prompt> prompts = new ArrayList<>();
    private Runnable onCall = () -> { };

    /** A reply with provider-reported token counts, as a real provider's usage metadata would carry. */
    public FakeChatModel reply(String text, int promptTokens, int completionTokens) {
        return respond(response(text, ChatResponseMetadata.builder()
                .usage(new DefaultUsage(promptTokens, completionTokens)).build()));
    }

    /** A reply whose provider reported no usage at all. */
    public FakeChatModel replyWithoutUsage(String text) {
        return respond(response(text, ChatResponseMetadata.builder().build()));
    }

    public FakeChatModel respond(ChatResponse response) {
        script.add(() -> response);
        return this;
    }

    public FakeChatModel fail(RuntimeException error) {
        script.add(() -> {
            throw error;
        });
        return this;
    }

    /** Runs during each call, e.g. to advance a test clock and simulate model latency. */
    public FakeChatModel onCall(Runnable hook) {
        this.onCall = hook;
        return this;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        onCall.run();
        if (script.isEmpty()) {
            throw new IllegalStateException("FakeChatModel has no scripted reply left");
        }
        return script.poll().get();
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public Prompt lastPrompt() {
        return prompts.getLast();
    }

    public void reset() {
        script.clear();
        prompts.clear();
        onCall = () -> { };
    }

    private static ChatResponse response(String text, ChatResponseMetadata metadata) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))), metadata);
    }
}
