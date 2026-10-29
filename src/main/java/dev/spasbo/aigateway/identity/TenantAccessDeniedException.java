package dev.spasbo.aigateway.identity;

/** The token is valid but its tenant cannot be used; the message is for logs, not for the caller. */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
