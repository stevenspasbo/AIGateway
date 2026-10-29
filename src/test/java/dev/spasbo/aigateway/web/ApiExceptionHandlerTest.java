package dev.spasbo.aigateway.web;

import dev.spasbo.aigateway.identity.TenantAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void deniedTenantIs403WithoutLeakingTheReason() {
        ProblemDetail problem = handler.tenantDenied(new TenantAccessDeniedException("tenant 123 not found"));

        assertThat(problem.getStatus()).isEqualTo(403);
        assertThat(problem.getTitle()).isEqualTo("Tenant not recognized");
        assertThat(problem.getDetail()).doesNotContain("123");
    }
}
