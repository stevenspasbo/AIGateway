package dev.spasbo.aigateway.chat;

public class UnknownModelException extends RuntimeException {

    private final String modelId;

    public UnknownModelException(String modelId) {
        super("Model not in allowlist: " + modelId);
        this.modelId = modelId;
    }

    public String modelId() {
        return modelId;
    }
}
