package io.omnidepot.core.api.upload;

import io.omnidepot.core.api.storage.Sha256Digest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Pure Java, state-serializable SHA-256 accumulator for resumable chunked upload verification.
 * State is serialized into a compact 108-byte array matching (H0..H7, count, bufOfs, buffer).
 */
@SuppressWarnings({"java:S3776", "java:S107"})
public class ChunkedDigestAccumulator {

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

    public static ChunkedDigestAccumulator create() {
        return new ChunkedDigestAccumulator();
    }

    public static ChunkedDigestAccumulator fromState(byte[] stateBytes) {
        Objects.requireNonNull(stateBytes, "State bytes must not be null");
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

    public void update(byte[] chunk) {
        Objects.requireNonNull(chunk, "Chunk must not be null");
        update(chunk, 0, chunk.length);
    }

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

    public Sha256Digest digest() {
        // Clone internal state for finalization so accumulator remains valid
        int[] h = state.clone();
        byte[] padBuf = Arrays.copyOf(buffer, BLOCK_SIZE);
        int pOfs = bufOfs;
        long totalBits = count * 8L;

        // Append 0x80 byte
        padBuf[pOfs++] = (byte) 0x80;

        if (pOfs > 56) {
            while (pOfs < BLOCK_SIZE) {
                padBuf[pOfs++] = 0;
            }
            processBlock(h, padBuf, 0);
            Arrays.fill(padBuf, (byte) 0);
            pOfs = 0;
        }

        while (pOfs < 56) {
            padBuf[pOfs++] = 0;
        }

        // Append bit count as 64-bit big endian int
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
