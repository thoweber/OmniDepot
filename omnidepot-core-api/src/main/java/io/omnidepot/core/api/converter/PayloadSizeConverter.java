package io.omnidepot.core.api.converter;

import org.jspecify.annotations.Nullable;

/**
 * Utility converter for human-readable payload array size formatting (e.g., "byte[](1.2MB)", "byte[](45.0KB)", "byte[](128B)").
 * Prevents dumping large binary array contents into log entries, toString representations, or diagnostics.
 */
public final class PayloadSizeConverter {

    private PayloadSizeConverter() {
        // Non-instantiable utility class
    }

    public static String formatPayload(@Nullable byte[] payload) {
        if (payload == null) {
            return "null";
        }
        return formatSize(payload.length);
    }

    public static String formatSize(long length) {
        if (length >= 1_048_576) {
            return String.format("byte[](%.1fMB)", length / 1_048_576.0);
        }
        if (length >= 1_024) {
            return String.format("byte[](%.1fKB)", length / 1_024.0);
        }
        return "byte[](" + length + "B)";
    }
}
