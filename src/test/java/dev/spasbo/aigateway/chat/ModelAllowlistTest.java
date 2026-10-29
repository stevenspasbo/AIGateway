package dev.spasbo.aigateway.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelAllowlistTest {

    final ModelAllowlist allowlist = new ModelAllowlist(List.of(
            new ModelDefinition("llama3.2:1b", "ollama"),
            new ModelDefinition("qwen3:0.6b", "ollama")));

    @Test
    void configuredModelsAreAllowed() {
        assertThat(allowlist.find("llama3.2:1b")).contains(new ModelDefinition("llama3.2:1b", "ollama"));
    }

    @Test
    void anythingElseIsNot() {
        assertThat(allowlist.find("gpt-4o")).isEmpty();
        assertThat(allowlist.find(null)).isEmpty();
    }

    @Test
    void listsModelsInConfiguredOrder() {
        assertThat(allowlist.all()).extracting(ModelDefinition::id).containsExactly("llama3.2:1b", "qwen3:0.6b");
    }

    @Test
    void duplicateIdsAreAConfigurationError() {
        assertThatThrownBy(() -> new ModelAllowlist(List.of(
                new ModelDefinition("llama3.2:1b", "ollama"), new ModelDefinition("llama3.2:1b", "bedrock"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("llama3.2:1b");
    }
}
