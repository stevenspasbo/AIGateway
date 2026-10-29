package dev.spasbo.aigateway.chat;

import dev.spasbo.aigateway.web.ApiSliceTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(ModelsController.class)
@Import(ChatConfig.class)
@TestPropertySource(properties = {"gateway.models[0].id=llama3.2:1b", "gateway.models[0].provider=ollama"})
class ModelsControllerTest extends ApiSliceTest {

    @Test
    void listsAllowedModelsWithTheirProvider() {
        var result = mvc.get().uri("/api/models").with(user()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                [{"id": "llama3.2:1b", "provider": "ollama"}]""");
    }
}
