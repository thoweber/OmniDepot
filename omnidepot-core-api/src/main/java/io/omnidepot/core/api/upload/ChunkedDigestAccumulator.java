package io.omnidepot.core.api.upload;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.jspecify.annotations.Nullable;

import java.util.HexFormat;
import java.util.Objects;

/**
 * Adapter wrapping Bouncy Castle's standard, enterprise-grade {@link SHA256Digest} for resumable chunked upload verification.
 *
 * <p>Delegates 100% of cryptographic operations, FIPS 180-4 message schedule expansions, compression rounds, and
 * intermediate state serialization to Bouncy Castle's audited {@link SHA256Digest} implementation.</p>
 */
public final class ChunkedDigestAccumulator {

    private final SHA256Digest delegate;

    private ChunkedDigestAccumulator(SHA256Digest delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new accumulator initialized to standard SHA-256 starting state.
     *
     * @return a fresh {@link ChunkedDigestAccumulator} instance
     */
    public static ChunkedDigestAccumulator create() {
        return new ChunkedDigestAccumulator(new SHA256Digest());
    }

    /**
     * Restores an accumulator instance from a serialized Bouncy Castle intermediate state payload.
     *
     * @param stateBytes the serialized state payload
     * @return a restored {@link ChunkedDigestAccumulator} ready for further chunk updates
     * @throws IllegalArgumentException if stateBytes is null or contains invalid state format
     */
    public static ChunkedDigestAccumulator fromState(byte @Nullable [] stateBytes) {
        if (stateBytes == null) {
            throw new IllegalArgumentException("Invalid digest state: stateBytes must not be null");
        }
        try {
            SHA256Digest restored = new SHA256Digest(stateBytes);
            return new ChunkedDigestAccumulator(restored);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid digest state: " + ex.getMessage(), ex);
        }
    }

    /**
     * Updates the accumulator with the full contents of the provided byte array chunk.
     *
     * @param chunk the byte array containing chunk payload
     * @throws NullPointerException if chunk is null
     */
    public void update(byte[] chunk) {
        Objects.requireNonNull(chunk, "Chunk must not be null");
        update(chunk, 0, chunk.length);
    }

    /**
     * Updates the accumulator with a slice of the provided byte array chunk.
     *
     * @param chunk the byte array chunk payload
     * @param offset the starting index in the chunk array
     * @param len the number of bytes to process
     * @throws NullPointerException if chunk is null
     * @throws IndexOutOfBoundsException if offset or len are invalid
     */
    public void update(byte[] chunk, int offset, int len) {
        Objects.requireNonNull(chunk, "Chunk must not be null");
        if (offset < 0 || len < 0 || offset + len > chunk.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        delegate.update(chunk, offset, len);
    }

    /**
     * Serializes the current intermediate SHA-256 computation state using Bouncy Castle's state encoding.
     *
     * @return serialized state byte array
     */
    public byte[] serializeState() {
        return delegate.getEncodedState();
    }

    /**
     * Finalizes the SHA-256 computation and returns the resulting digest.
     * <p>Clones the underlying Bouncy Castle digest so that this accumulator instance remains valid
     * for further updates if needed.</p>
     *
     * @return immutable {@link Sha256Digest} object wrapping hex representation
     */
    public Sha256Digest digest() {
        SHA256Digest copy = new SHA256Digest(delegate);
        byte[] hash = new byte[32];
        copy.doFinal(hash, 0);
        return Sha256Digest.of(HexFormat.of().formatHex(hash));
    }
}
