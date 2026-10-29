package dev.spasbo.aigateway.chat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayPropertiesTest {

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(ChatConfig.class);

    @Test
    void bindsTheAllowlistFromConfig() {
        runner.withPropertyValues("gateway.models[0].id=llama3.2:1b", "gateway.models[0].provider=ollama")
                .run(ctx -> assertThat(ctx.getBean(ModelAllowlist.class).find("llama3.2:1b")).isPresent());
    }

    @Test
    void emptyAllowlistFailsStartup() {
        runner.run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void modelWithoutProviderFailsStartup() {
        runner.withPropertyValues("gateway.models[0].id=llama3.2:1b")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
