package io.omnidepot.core.api.iam;

import io.smallrye.mutiny.Uni;
import java.util.Set;

/**
 * IAM SPI for exchanging Personal Access Tokens (PAT) for short-lived JWT tokens
 * and validating authentication scopes for CLI operations.
 */
public interface TokenBroker {

    /**
     * Issue a short-lived JWT token in exchange for a PAT token and requested scopes.
     */
    Uni<String> issueTokenForPat(String pat, Set<String> requestedScopes, String repository);

    /**
     * Validate a bearer token and parse its claims.
     */
    Uni<TokenClaims> validateToken(String jwtToken);

    /**
     * Check whether token claims grant access for a required scope and repository.
     */
    boolean hasScope(TokenClaims claims, String requiredScope, String repository);
}
