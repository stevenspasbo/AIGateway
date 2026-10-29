package dev.spasbo.aigateway.chat;

/** The provider call failed or returned nothing usable. Maps to 502; the cause is logged, never returned. */
public class ModelCallFailedException extends RuntimeException {

    private final String modelId;

    public ModelCallFailedException(String modelId, Throwable cause) {
        super("Call to model " + modelId + " failed", cause);
        this.modelId = modelId;
    }

    public ModelCallFailedException(String modelId, String reason) {
        super("Call to model " + modelId + " failed: " + reason);
        this.modelId = modelId;
    }

    public String modelId() {
        return modelId;
    }
}
