package io.omnidepot.core.api.iam;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Value Object representing parsed security token claims and granted scope capabilities.
 */
public record TokenClaims(
        String subject,
        String username,
        Set<String> scopes,
        Instant issuedAt,
        Instant expiresAt
) {
    public TokenClaims {
        Objects.requireNonNull(subject, "Subject must not be null");
        Objects.requireNonNull(scopes, "Scopes must not be null");
        Objects.requireNonNull(issuedAt, "IssuedAt must not be null");
        Objects.requireNonNull(expiresAt, "ExpiresAt must not be null");
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
