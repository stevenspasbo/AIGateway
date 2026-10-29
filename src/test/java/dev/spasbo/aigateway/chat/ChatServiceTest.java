package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.identity.CurrentUser;
import dev.spasbo.aigateway.identity.Role;
import dev.spasbo.aigateway.metering.CallStatus;
import dev.spasbo.aigateway.metering.MeteringService;
import dev.spasbo.aigateway.metering.ModelCall;
import dev.spasbo.aigateway.testsupport.FakeChatModel;
import dev.spasbo.aigateway.testsupport.MutableClock;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatServiceTest {

    static final ModelDefinition LLAMA = new ModelDefinition("llama3.2:1b", "ollama");
    static final CurrentUser ALICE = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "sub-1", Role.USER);

    final MutableClock clock = new MutableClock(Instant.parse("2026-10-28T12:00:00Z"));
    final FakeChatModel model = new FakeChatModel().onCall(() -> clock.advance(Duration.ofMillis(420)));
    final MeteringService metering = mock(MeteringService.class);
    final ChatService service = new ChatService(model, metering, clock);

    @Test
    void returnsTheReplyWithProviderReportedTokens() {
        model.reply("Hello from the fake", 12, 5);

        ChatResult result = service.chat(ALICE, LLAMA, "Hi");

        assertThat(result).isEqualTo(new ChatResult("Hello from the fake", "llama3.2:1b", 12, 5, 420));
    }

    @Test
    void metersEachSuccessfulCall() {
        model.reply("Hello", 12, 5);

        service.chat(ALICE, LLAMA, "Hi");

        verify(metering).record(new ModelCall(ALICE.tenantId(), ALICE.userId(), "llama3.2:1b", 12, 5, 420, CallStatus.SUCCESS));
    }

    @Test
    void failedCallIsMeteredAsErrorWithZeroTokensAndRealLatency() {
        model.fail(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> service.chat(ALICE, LLAMA, "Hi"))
                .isInstanceOf(ModelCallFailedException.class)
                .hasRootCauseInstanceOf(ResourceAccessException.class);

        verify(metering).record(new ModelCall(ALICE.tenantId(), ALICE.userId(), "llama3.2:1b", 0, 0, 420, CallStatus.ERROR));
    }

    @Test
    void responseWithoutAGenerationIsAFailure() {
        model.respond(new ChatResponse(List.of()));

        assertThatThrownBy(() -> service.chat(ALICE, LLAMA, "Hi")).isInstanceOf(ModelCallFailedException.class);

        verify(metering).record(new ModelCall(ALICE.tenantId(), ALICE.userId(), "llama3.2:1b", 0, 0, 420, CallStatus.ERROR));
    }

    @Test
    void sendsTheMessageAsASingleUserTurn() {
        model.reply("ok", 1, 1);

        service.chat(ALICE, LLAMA, "What is FedRAMP?");

        var instructions = model.lastPrompt().getInstructions();
        assertThat(instructions).hasSize(1);
        assertThat(instructions.getFirst().getMessageType()).isEqualTo(MessageType.USER);
        assertThat(instructions.getFirst().getText()).isEqualTo("What is FedRAMP?");
    }

    @Test
    void requestsTheChosenModelPerCall() {
        model.reply("ok", 1, 1);

        service.chat(ALICE, new ModelDefinition("qwen3:0.6b", "ollama"), "Hi");

        assertThat(model.lastPrompt().getOptions().getModel()).isEqualTo("qwen3:0.6b");
    }

    @Test
    void missingUsageCountsAsZeroTokensNotAnEstimate() {
        model.replyWithoutUsage("ok");

        ChatResult result = service.chat(ALICE, LLAMA, "Hi");

        assertThat(result.inputTokens()).isZero();
        assertThat(result.outputTokens()).isZero();
    }
}
