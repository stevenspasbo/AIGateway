package dev.spasbo.aigateway.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Runs after authorization: turns the JWT into a {@link CurrentUser} for the request, or rejects the
 * request with 403 when the tenant is unusable. Errors go through the MVC exception resolver so they
 * share the RFC 9457 problem-detail shape with every other API error.
 */
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

    private final IdentityService identity;
    private final HandlerExceptionResolver exceptionResolver;

    public TenantResolutionFilter(IdentityService identity, HandlerExceptionResolver exceptionResolver) {
        this.identity = identity;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            try {
                identity.resolve(token.getToken()).bindTo(request);
            } catch (TenantAccessDeniedException e) {
                log.warn("Rejected request from subject {}: {}", token.getName(), e.getMessage());
                exceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
