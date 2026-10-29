package dev.spasbo.aigateway.web;

import dev.spasbo.aigateway.chat.ModelCallFailedException;
import dev.spasbo.aigateway.chat.UnknownModelException;
import dev.spasbo.aigateway.identity.TenantAccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps gateway exceptions to RFC 9457 problem details. Extending {@link ResponseEntityExceptionHandler}
 * gives validation and other framework errors the same shape.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ProblemDetail modelCallFailed(ModelCallFailedException e) {
        log.error("Model call failed for {}", e.modelId(), e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "The model provider could not complete this request. Try again shortly.");
        problem.setTitle("Model provider error");
        problem.setProperty("model", e.modelId());
        return problem;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail unknownModel(UnknownModelException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "This model is not available. GET /api/models lists the models you can use.");
        problem.setTitle("Model not allowed");
        problem.setProperty("model", e.modelId());
        return problem;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ProblemDetail tenantDenied(TenantAccessDeniedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Your organization is not registered with this gateway. Contact your administrator.");
        problem.setTitle("Tenant not recognized");
        return problem;
    }
}
