package dev.spasbo.aigateway.identity;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserArgumentResolverTest {

    final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();
    final MockHttpServletRequest request = new MockHttpServletRequest();

    @SuppressWarnings("unused")
    void handler(CurrentUser user, String other) {
    }

    @Test
    void supportsOnlyCurrentUserParameters() throws Exception {
        var method = getClass().getDeclaredMethod("handler", CurrentUser.class, String.class);

        assertThat(resolver.supportsParameter(new MethodParameter(method, 0))).isTrue();
        assertThat(resolver.supportsParameter(new MethodParameter(method, 1))).isFalse();
    }

    @Test
    void resolvesTheUserBoundByTheFilter() {
        var user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "sub-1", Role.USER);
        user.bindTo(request);

        assertThat(resolver.resolveArgument(null, null, new ServletWebRequest(request), null)).isEqualTo(user);
    }

    @Test
    void failsLoudlyWhenNothingWasBound() {
        assertThatThrownBy(() -> resolver.resolveArgument(null, null, new ServletWebRequest(request), null))
                .isInstanceOf(IllegalStateException.class);
    }
}
