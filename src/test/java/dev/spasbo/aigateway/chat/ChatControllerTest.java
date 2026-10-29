package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.web.ApiSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(ChatController.class)
@Import(ChatConfig.class)
@TestPropertySource(properties = {"gateway.models[0].id=llama3.2:1b", "gateway.models[0].provider=ollama"})
class ChatControllerTest extends ApiSliceTest {

    static final ModelDefinition LLAMA = new ModelDefinition("llama3.2:1b", "ollama");

    @MockitoBean
    ChatService chatService;

    @Test
    void returnsTheCompleteReplyWithUsage() {
        when(chatService.chat(CALLER, LLAMA, "Hi")).thenReturn(new ChatResult("Hello!", "llama3.2:1b", 12, 5, 420));

        var result = post("""
                {"model": "llama3.2:1b", "message": "Hi"}""");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isStrictlyEqualTo("""
                {"reply": "Hello!", "model": "llama3.2:1b", "inputTokens": 12, "outputTokens": 5, "latencyMs": 420}""");
    }

    @Test
    void callerComesFromTheTokenNotTheBody() {
        when(chatService.chat(any(), any(), any())).thenReturn(new ChatResult("ok", "llama3.2:1b", 1, 1, 1));

        post("""
                {"model": "llama3.2:1b", "message": "Hi", "tenantId": "00000000-0000-0000-0000-000000000000"}""");

        verify(chatService).chat(CALLER, LLAMA, "Hi");
    }

    @Test
    void modelOutsideTheAllowlistIs400AndNeverReachesAProvider() {
        var result = post("""
                {"model": "gpt-4o", "message": "Hi"}""");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Model not allowed");
        assertThat(result).bodyJson().extractingPath("$.model").isEqualTo("gpt-4o");
        verifyNoInteractions(chatService);
    }

    @Test
    void blankMessageIs400ProblemDetail() {
        var result = post("""
                {"model": "llama3.2:1b", "message": "  "}""");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        verifyNoInteractions(chatService);
    }

    @Test
    void missingModelIs400() {
        assertThat(post("""
                {"message": "Hi"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chatService);
    }

    @Test
    void oversizedMessageIs400() {
        String tooLong = "a".repeat(ChatController.MAX_MESSAGE_CHARS + 1);

        assertThat(post("{\"model\": \"llama3.2:1b\", \"message\": \"" + tooLong + "\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chatService);
    }

    @Test
    void providerFailureIs502ProblemDetail() {
        when(chatService.chat(any(), any(), any())).thenThrow(new ModelCallFailedException("llama3.2:1b", "boom"));

        var result = post("""
                {"model": "llama3.2:1b", "message": "Hi"}""");

        assertThat(result).hasStatus(HttpStatus.BAD_GATEWAY);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Model provider error");
    }

    private org.springframework.test.web.servlet.assertj.MvcTestResult post(String body) {
        return mvc.post().uri("/api/chat").with(user()).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
