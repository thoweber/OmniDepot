package io.omnidepot.core.api.storage;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when deleting a blob from Content-Addressable Storage fails.
 */
public class BlobDeletionException extends StorageException {

    public BlobDeletionException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
