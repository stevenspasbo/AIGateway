package dev.spasbo.aigateway.web;

import dev.spasbo.aigateway.config.SecurityConfig;
import dev.spasbo.aigateway.identity.CurrentUser;
import dev.spasbo.aigateway.identity.IdentityService;
import dev.spasbo.aigateway.identity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Base for {@code @WebMvcTest} slices: real security chain, mocked token decoding and identity lookup. */
@Import(SecurityConfig.class)
public abstract class ApiSliceTest {

    protected static final CurrentUser CALLER =
            new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "sub-1", Role.USER);

    @Autowired
    protected MockMvcTester mvc;

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected IdentityService identityService;

    @BeforeEach
    void resolveEveryCaller() {
        when(identityService.resolve(any())).thenReturn(CALLER);
    }

    protected static JwtRequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority(Role.USER.authority()));
    }
}
