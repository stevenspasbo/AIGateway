package dev.spasbo.aigateway.config;

import dev.spasbo.aigateway.identity.IdentityService;
import dev.spasbo.aigateway.identity.JwtRolesConverter;
import dev.spasbo.aigateway.identity.Role;
import dev.spasbo.aigateway.identity.TenantResolutionFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, IdentityService identity,
                                    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRolesConverter())))
                // Not a bean, so Boot does not also register it as a plain servlet filter
                .addFilterAfter(new TenantResolutionFilter(identity, exceptionResolver), AuthorizationFilter.class)
                // Bearer tokens only: no cookies, so no session and no CSRF exposure
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
