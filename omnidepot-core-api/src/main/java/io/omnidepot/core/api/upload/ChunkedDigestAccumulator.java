package io.omnidepot.core.api.upload;

import io.omnidepot.core.api.storage.Sha256Digest;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Pure Java 25, state-serializable SHA-256 digest accumulator enforcing FIPS 180-4 standard specification.
 *
 * <p>Designed specifically for OCI Distribution spec compliant resumable chunked blob uploads.
 * Standard JDK {@link java.security.MessageDigest} instances are non-serializable, preventing state persistence
 * across HTTP request boundaries during pod restarts or distributed chunk uploads. This implementation extracts
 * and serializes the exact 108-byte intermediate SHA-256 computation state between chunk arrivals.</p>
 *
 * <h3>108-Byte State Binary Layout:</h3>
 * <pre>{@text
 * +-------------------+------------------+------------------+-------------------+
 * |  H0..H7 (32 B)    |    Count (8 B)   |   BufOfs (4 B)   |   Buffer (64 B)   |
 * |  8 x int32 (BE)   |   int64 long (BE)|   int32 (BE)     |   unprocessed tail|
 * +-------------------+------------------+------------------+-------------------+
 * 0                   32                 40                 44                 108
 * }</pre>
 *
 * <h3>Algorithm Overview (FIPS 180-4):</h3>
 * <ul>
 *   <li><b>Block Size:</b> 512 bits (64 bytes).</li>
 *   <li><b>State Initialization:</b> Standard fractional square roots of first 8 primes ($H_0 \dots H_7$).</li>
 *   <li><b>Message Schedule Expansion ($W_0 \dots W_{63}$):</b> 16 input words expanded into 64 words using $\sigma_0$ and $\sigma_1$ rotations.</li>
 *   <li><b>Compression Function:</b> 64 rounds of non-linear bitwise operations using $K_0 \dots K_{63}$ constants.</li>
 *   <li><b>Finalization & Padding:</b> Appends {@code 0x80} bit, zero-pads up to 448 bits (or dual block if tail exceeds 448 bits), and appends 64-bit total bit count.</li>
 * </ul>
 */
@SuppressWarnings({"java:S3776", "java:S107"})
public final class ChunkedDigestAccumulator {

    private static final int BLOCK_SIZE = 64;

    private static final int[] K = {
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private final int[] state = new int[8];
    private final byte[] buffer = new byte[BLOCK_SIZE];
    private final int[] w = new int[64];

    private long count; // Total bytes processed
    private int bufOfs; // Current offset in buffer

    private ChunkedDigestAccumulator() {
        resetState();
    }

    private void resetState() {
        state[0] = 0x6a09e667;
        state[1] = 0xbb67ae85;
        state[2] = 0x3c6ef372;
        state[3] = 0xa54ff53a;
        state[4] = 0x510e527f;
        state[5] = 0x9b05688c;
        state[6] = 0x1f83d9ab;
        state[7] = 0x5be0cd19;
        count = 0;
        bufOfs = 0;
        Arrays.fill(buffer, (byte) 0);
    }

    /**
     * Creates a new accumulator initialized to standard SHA-256 starting state ($H_0 \dots H_7$).
     *
     * @return a fresh {@link ChunkedDigestAccumulator} instance
     */
    public static ChunkedDigestAccumulator create() {
        return new ChunkedDigestAccumulator();
    }

    /**
     * Restores an accumulator instance from a serialized 108-byte intermediate state.
     *
     * @param stateBytes the 108-byte serialized state payload
     * @return a restored {@link ChunkedDigestAccumulator} ready for further chunk updates
     * @throws IllegalArgumentException if stateBytes is null, not 108 bytes, or contains corrupted state bounds
     */
    public static ChunkedDigestAccumulator fromState(byte @Nullable [] stateBytes) {
        if (stateBytes == null) {
            throw new IllegalArgumentException("Invalid digest state: stateBytes must not be null");
        }
        if (stateBytes.length != 108) {
            throw new IllegalArgumentException("Invalid digest state: state length must be exactly 108 bytes, got " + stateBytes.length);
        }
        ChunkedDigestAccumulator acc = new ChunkedDigestAccumulator();
        ByteBuffer bb = ByteBuffer.wrap(stateBytes);
        for (int i = 0; i < 8; i++) {
            acc.state[i] = bb.getInt();
        }
        acc.count = bb.getLong();
        acc.bufOfs = bb.getInt();
        bb.get(acc.buffer, 0, BLOCK_SIZE);

        if (acc.bufOfs < 0 || acc.bufOfs >= BLOCK_SIZE || acc.count < 0) {
            throw new IllegalArgumentException("Invalid digest state: corrupted header bounds");
        }
        return acc;
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

        int pos = offset;
        int remaining = len;

        while (remaining > 0) {
            int toCopy = Math.min(remaining, BLOCK_SIZE - bufOfs);
            System.arraycopy(chunk, pos, buffer, bufOfs, toCopy);
            bufOfs += toCopy;
            pos += toCopy;
            remaining -= toCopy;
            count += toCopy;

            if (bufOfs == BLOCK_SIZE) {
                processBlock(buffer, 0);
                bufOfs = 0;
            }
        }
    }

    /**
     * Serializes the current intermediate SHA-256 computation state into a compact 108-byte array.
     *
     * @return 108-byte array representing intermediate digest computation state
     */
    public byte[] serializeState() {
        ByteBuffer bb = ByteBuffer.allocate(108);
        for (int i = 0; i < 8; i++) {
            bb.putInt(state[i]);
        }
        bb.putLong(count);
        bb.putInt(bufOfs);
        bb.put(buffer, 0, BLOCK_SIZE);
        return bb.array();
    }

    /**
     * Finalizes the SHA-256 computation (applying FIPS 180-4 padding) and returns the resulting digest.
     * <p>Note: This call clones internal state during computation, allowing the accumulator instance
     * to remain valid for further updates if needed.</p>
     *
     * @return immutable {@link Sha256Digest} object wrapping hex representation
     */
    public Sha256Digest digest() {
        int[] h = state.clone();
        byte[] padBuf = Arrays.copyOf(buffer, BLOCK_SIZE);
        int pOfs = bufOfs;
        long totalBits = count * 8L;

        padBuf[pOfs] = (byte) 0x80;
        pOfs++;

        if (pOfs > 56) {
            while (pOfs < BLOCK_SIZE) {
                padBuf[pOfs] = 0;
                pOfs++;
            }
            processBlock(h, padBuf, 0);
            Arrays.fill(padBuf, (byte) 0);
            pOfs = 0;
        }

        while (pOfs < 56) {
            padBuf[pOfs] = 0;
            pOfs++;
        }

        ByteBuffer.wrap(padBuf, 56, 8).putLong(totalBits);
        processBlock(h, padBuf, 0);

        byte[] hash = new byte[32];
        ByteBuffer hb = ByteBuffer.wrap(hash);
        for (int i = 0; i < 8; i++) {
            hb.putInt(h[i]);
        }

        return Sha256Digest.of(HexFormat.of().formatHex(hash));
    }

    private void processBlock(byte[] block, int offset) {
        processBlock(state, block, offset);
    }

    private void processBlock(int[] h, byte[] block, int offset) {
        ByteBuffer bb = ByteBuffer.wrap(block, offset, BLOCK_SIZE);
        for (int i = 0; i < 16; i++) {
            w[i] = bb.getInt();
        }
        for (int i = 16; i < 64; i++) {
            int s0 = Integer.rotateRight(w[i - 15], 7) ^ Integer.rotateRight(w[i - 15], 18) ^ (w[i - 15] >>> 3);
            int s1 = Integer.rotateRight(w[i - 2], 17) ^ Integer.rotateRight(w[i - 2], 19) ^ (w[i - 2] >>> 10);
            w[i] = w[i - 16] + s0 + w[i - 7] + s1;
        }

        int a = h[0];
        int b = h[1];
        int c = h[2];
        int d = h[3];
        int e = h[4];
        int f = h[5];
        int g = h[6];
        int hVal = h[7];

        for (int i = 0; i < 64; i++) {
            int sigma1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
            int ch = (e & f) ^ ((~e) & g);
            int temp1 = hVal + sigma1 + ch + K[i] + w[i];
            int sigma0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
            int maj = (a & b) ^ (a & c) ^ (b & c);
            int temp2 = sigma0 + maj;

            hVal = g;
            g = f;
            f = e;
            e = d + temp1;
            d = c;
            c = b;
            b = a;
            a = temp1 + temp2;
        }

        h[0] += a;
        h[1] += b;
        h[2] += c;
        h[3] += d;
        h[4] += e;
        h[5] += f;
        h[6] += g;
        h[7] += hVal;
    }
}
