package dev.spasbo.aigateway.chat;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProperties.class)
public class ChatConfig {

    @Bean
    ModelAllowlist modelAllowlist(GatewayProperties properties) {
        return new ModelAllowlist(properties.models());
    }
}
