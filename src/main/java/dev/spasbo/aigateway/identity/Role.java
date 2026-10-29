package dev.spasbo.aigateway.identity;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/** Gateway roles, ordered lowest to highest. The token's {@code roles} claim uses the lowercase names. */
public enum Role {
    USER,
    ADMIN;

    public String claimValue() {
        return name().toLowerCase();
    }

    public String authority() {
        return "ROLE_" + name();
    }

    public static Stream<Role> fromClaims(Collection<String> claimValues) {
        return Stream.of(values()).filter(role -> claimValues.contains(role.claimValue()));
    }

    /** app_user stores a single role, so a user holding both is recorded as ADMIN. */
    public static Optional<Role> highestOf(Collection<String> claimValues) {
        return fromClaims(claimValues).reduce((lower, higher) -> higher);
    }
}
