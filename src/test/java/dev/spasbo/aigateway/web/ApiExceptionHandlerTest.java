package dev.spasbo.aigateway.web;

import dev.spasbo.aigateway.chat.ModelCallFailedException;
import dev.spasbo.aigateway.identity.TenantAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void failedModelCallIs502WithoutProviderInternals() {
        var cause = new RuntimeException("I/O error on POST http://ollama:11434/api/chat: secret-host refused");

        ProblemDetail problem = handler.modelCallFailed(new ModelCallFailedException("llama3.2:1b", cause));

        assertThat(problem.getStatus()).isEqualTo(502);
        assertThat(problem.getTitle()).isEqualTo("Model provider error");
        assertThat(problem.getDetail()).doesNotContain("secret-host").doesNotContain("11434");
        assertThat(problem.getProperties()).containsEntry("model", "llama3.2:1b");
    }

    @Test
    void deniedTenantIs403WithoutLeakingTheReason() {
        ProblemDetail problem = handler.tenantDenied(new TenantAccessDeniedException("tenant 123 not found"));

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getTitle()).isEqualTo("Tenant not recognized");
        assertThat(problem.getDetail()).doesNotContain("123");
    }
}
