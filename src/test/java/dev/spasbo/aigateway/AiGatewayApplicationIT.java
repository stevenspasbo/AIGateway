package dev.spasbo.aigateway;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.retry.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The real application context and configuration, with the real Ollama adapter wired but never called. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AiGatewayApplicationIT {

    @Autowired
    ChatModel chatModel;

    @Autowired
    RetryTemplate aiRetryTemplate;

    @Test
    void ollamaIsTheProviderWithTheConfiguredDefaultModel() {
        assertThat(chatModel).isInstanceOf(OllamaChatModel.class);
        assertThat(chatModel.getOptions().getModel()).isEqualTo("llama3.2:1b");
    }

    @Test
    void perRequestOptionsKeepTheProviderOptionsType() {
        // OllamaChatModel casts prompt options to OllamaChatOptions; portable ChatOptions would fail at call time
        var options = chatModel.getOptions().mutate().model("qwen3:0.6b").build();

        assertThat(options).isInstanceOf(OllamaChatOptions.class);
        assertThat(options.getModel()).isEqualTo("qwen3:0.6b");
    }

    @Test
    void springAiRetriesAreOffInMilestoneOne() {
        var attempts = new AtomicInteger();

        assertThatThrownBy(() -> aiRetryTemplate.execute(() -> {
            if (attempts.incrementAndGet() > 1) {
                throw new IllegalStateException("retried"); // not retryable, so a misconfiguration stops here
            }
            throw new TransientAiException("provider hiccup");
        }));

        assertThat(attempts).hasValue(1);
    }
}
