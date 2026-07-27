package io.omnidepot.core.api.storage;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when reading or opening a stream for a blob in Content-Addressable Storage fails.
 */
public class BlobReadException extends StorageException {

    public BlobReadException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
