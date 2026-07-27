# Security & IAM SPI

OmniDepot provides identity management, authentication, and scoped token issuance via the core `TokenBroker` SPI.

---

## Core Interfaces

### `TokenBroker`
```java
package io.omnidepot.core.api.iam;

import io.smallrye.mutiny.Uni;

public interface TokenBroker {
    Uni<String> issueToken(String principal, String scope, long ttlSeconds);
    Uni<TokenClaims> validateToken(String token);
}
```

### `TokenClaims`
```java
package io.omnidepot.core.api.iam;

import java.time.Instant;
import java.util.Set;

public record TokenClaims(
    String principal,
    String scope,
    Set<String> permissions,
    Instant issuedAt,
    Instant expiresAt
) {}
```
