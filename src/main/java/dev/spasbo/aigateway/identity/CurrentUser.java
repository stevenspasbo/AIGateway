package dev.spasbo.aigateway.identity;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/** The caller as the gateway knows them: always derived from the JWT, never from the request body. */
public record CurrentUser(UUID tenantId, UUID userId, String subject, Role role) {

    static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

    public static CurrentUser from(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute(REQUEST_ATTRIBUTE);
    }

    void bindTo(HttpServletRequest request) {
        request.setAttribute(REQUEST_ATTRIBUTE, this);
    }
}
