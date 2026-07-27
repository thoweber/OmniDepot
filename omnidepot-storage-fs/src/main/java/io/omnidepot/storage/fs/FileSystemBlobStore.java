package io.omnidepot.storage.fs;

import io.omnidepot.core.api.storage.BlobDeletionException;
import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobReadException;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.BlobWriteException;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Filesystem implementation of Content-Addressable Storage BlobStore SPI (ADR-004, ADR-015).
 */
@ApplicationScoped
public class FileSystemBlobStore implements BlobStore {

    private final Path storageRoot;

    public FileSystemBlobStore(@ConfigProperty(name = "omnidepot.storage.fs.root-dir", defaultValue = "./target/omnidepot-storage") String rootDir) {
        this.storageRoot = Paths.get(rootDir).toAbsolutePath().normalize();
    }

    private Path resolvePath(Sha256Digest digest) {
        return storageRoot.resolve(digest.toCasPath().value());
    }

    @Override
    public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream data, long sizeBytes) {
        return Uni.createFrom().item(() -> writeBlobToDisk(digest, mediaType, data));
    }

    private BlobDescriptor writeBlobToDisk(Sha256Digest digest, String mediaType, InputStream data) {
        Path targetPath = resolvePath(digest);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(data, targetPath, StandardCopyOption.REPLACE_EXISTING);
            long actualSize = Files.size(targetPath);
            return new BlobDescriptor(
                    UUID.randomUUID().toString(),
                    digest,
                    actualSize,
                    mediaType,
                    targetPath.toString(),
                    Instant.now()
            );
        } catch (IOException e) {
            throw new BlobWriteException("Failed to write blob to filesystem CAS for digest: " + digest, e);
        }
    }

    @Override
    public Uni<InputStream> openStream(Sha256Digest digest) {
        return Uni.createFrom().item(() -> {
            Path targetPath = resolvePath(digest);
            try {
                return Files.newInputStream(targetPath);
            } catch (IOException e) {
                throw new BlobReadException("Failed to open stream for blob: " + digest, e);
            }
        });
    }

    @Override
    public Uni<Boolean> exists(Sha256Digest digest) {
        return Uni.createFrom().item(() -> Files.exists(resolvePath(digest)));
    }

    @Override
    public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
        return Uni.createFrom().item(() -> readDescriptorSafely(resolvePath(digest), digest));
    }

    private Optional<BlobDescriptor> readDescriptorSafely(Path targetPath, Sha256Digest digest) {
        if (!Files.exists(targetPath)) {
            return Optional.empty();
        }
        try {
            long size = Files.size(targetPath);
            return Optional.of(new BlobDescriptor(
                    UUID.randomUUID().toString(),
                    digest,
                    size,
                    "application/octet-stream",
                    targetPath.toString(),
                    Files.getLastModifiedTime(targetPath).toInstant()
            ));
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    @Override
    public Uni<Boolean> delete(Sha256Digest digest) {
        return Uni.createFrom().item(() -> deleteBlobFromDisk(digest));
    }

    private boolean deleteBlobFromDisk(Sha256Digest digest) {
        try {
            return Files.deleteIfExists(resolvePath(digest));
        } catch (IOException e) {
            throw new BlobDeletionException("Failed to delete blob for digest: " + digest, e);
        }
    }
}
