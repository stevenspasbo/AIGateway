package dev.spasbo.aigateway.identity;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controllers take a {@link CurrentUser} parameter, filled from what {@link TenantResolutionFilter} bound. */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == CurrentUser.class;
    }

    @Override
    public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                       NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        CurrentUser user = CurrentUser.from(webRequest.getNativeRequest(HttpServletRequest.class));
        if (user == null) {
            throw new IllegalStateException("No CurrentUser bound; is the endpoint outside /api/**?");
        }
        return user;
    }
}
