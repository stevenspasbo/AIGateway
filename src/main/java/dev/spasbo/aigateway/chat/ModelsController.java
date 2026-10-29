package dev.spasbo.aigateway.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final ModelAllowlist allowlist;

    public ModelsController(ModelAllowlist allowlist) {
        this.allowlist = allowlist;
    }

    @GetMapping
    List<ModelDefinition> list() {
        return allowlist.all();
    }
}
