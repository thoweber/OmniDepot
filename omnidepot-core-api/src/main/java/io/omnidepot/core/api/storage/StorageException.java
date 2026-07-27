package io.omnidepot.core.api.storage;

import org.jspecify.annotations.Nullable;

/**
 * Base domain runtime exception for Content-Addressable Storage (CAS) read, write, stream, and deletion failures.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
