package io.omnidepot.core.api.test.support;

import io.omnidepot.core.api.storage.Sha256Digest;

/**
 * ObjectMother pattern (Martin Fowler) for creating standardized Sha256Digest instances for testing.
 */
public final class DigestObjectMother {

    public static final String SAMPLE_SHA256_HEX = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    public static final String ALTERNATE_SHA256_HEX = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

    private DigestObjectMother() {}

    public static Sha256Digest emptyPayloadDigest() {
        return Sha256Digest.of(SAMPLE_SHA256_HEX);
    }

    public static Sha256Digest alternateDigest() {
        return Sha256Digest.of(ALTERNATE_SHA256_HEX);
    }

    public static Sha256Digest prefixedDigest() {
        return Sha256Digest.of("sha256:" + SAMPLE_SHA256_HEX);
    }

    public static Sha256Digest uppercaseDigest() {
        return Sha256Digest.of(SAMPLE_SHA256_HEX.toUpperCase());
    }
}
