package dev.spasbo.aigateway.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Replaces the provider in full-application tests; pair with {@code spring.ai.model.chat=none}. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeChatModelConfig {

    @Bean
    FakeChatModel fakeChatModel() {
        return new FakeChatModel();
    }
}
