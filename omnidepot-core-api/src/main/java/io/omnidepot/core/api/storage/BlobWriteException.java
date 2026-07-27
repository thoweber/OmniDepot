package io.omnidepot.core.api.storage;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when writing a blob to Content-Addressable Storage fails.
 */
public class BlobWriteException extends StorageException {

    public BlobWriteException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
