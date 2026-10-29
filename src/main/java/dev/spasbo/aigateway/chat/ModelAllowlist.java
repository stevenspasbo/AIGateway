package dev.spasbo.aigateway.chat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Models callers may use. Anything not listed is rejected before a provider is ever called. */
public class ModelAllowlist {

    private final Map<String, ModelDefinition> byId = new LinkedHashMap<>();

    public ModelAllowlist(List<ModelDefinition> models) {
        for (ModelDefinition model : models) {
            if (byId.putIfAbsent(model.id(), model) != null) {
                throw new IllegalArgumentException("Duplicate model id in gateway.models: " + model.id());
            }
        }
    }

    public Optional<ModelDefinition> find(String modelId) {
        return Optional.ofNullable(modelId).map(byId::get);
    }

    public List<ModelDefinition> all() {
        return List.copyOf(byId.values());
    }
}
