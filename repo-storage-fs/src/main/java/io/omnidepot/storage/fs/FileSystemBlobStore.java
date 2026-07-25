package io.omnidepot.storage.fs;

import io.omnidepot.core.api.storage.BlobDescriptor;
import io.omnidepot.core.api.storage.BlobStore;
import io.omnidepot.core.api.storage.Sha256Digest;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Local filesystem implementation of Content-Addressable Storage (CAS) BlobStore.
 * Package-private access to enforce boundary rules (ADR-009).
 */
@ApplicationScoped
@LookupIfProperty(name = "repo.storage.type", stringValue = "fs", lookupIfMissing = true)
class FileSystemBlobStore implements BlobStore {

    private final Path storageRoot;

    FileSystemBlobStore(@ConfigProperty(name = "repo.storage.fs.root-dir", defaultValue = "./data/blobs") String rootDir) {
        this.storageRoot = Path.of(rootDir).toAbsolutePath().normalize();
    }

    private Path resolvePath(Sha256Digest digest) {
        String hex = digest.hexValue();
        return storageRoot.resolve("sha256").resolve(hex.substring(0, 2)).resolve(hex.substring(2, 4)).resolve(hex);
    }

    @Override
    public Uni<BlobDescriptor> put(Sha256Digest digest, String mediaType, InputStream data, long sizeBytes) {
        return Uni.createFrom().item(() -> {
            try {
                Path targetPath = resolvePath(digest);
                Files.createDirectories(targetPath.getParent());

                Path tempFile = Files.createTempFile(targetPath.getParent(), "upload-", ".tmp");
                Files.copy(data, tempFile, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempFile, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                long actualSize = Files.size(targetPath);
                return new BlobDescriptor(
                        UUID.randomUUID().toString(),
                        digest,
                        actualSize,
                        mediaType,
                        targetPath.toString(),
                        Instant.now()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to write blob to filesystem", e);
            }
        });
    }

    @Override
    public Uni<InputStream> openStream(Sha256Digest digest) {
        return Uni.createFrom().item(() -> {
            try {
                Path targetPath = resolvePath(digest);
                if (!Files.exists(targetPath)) {
                    throw new IllegalArgumentException("Blob not found: " + digest);
                }
                return Files.newInputStream(targetPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to open blob stream", e);
            }
        });
    }

    @Override
    public Uni<Boolean> exists(Sha256Digest digest) {
        return Uni.createFrom().item(() -> Files.exists(resolvePath(digest)));
    }

    @Override
    public Uni<Optional<BlobDescriptor>> getDescriptor(Sha256Digest digest) {
        return Uni.createFrom().item(() -> {
            Path targetPath = resolvePath(digest);
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
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Uni<Boolean> delete(Sha256Digest digest) {
        return Uni.createFrom().item(() -> {
            try {
                return Files.deleteIfExists(resolvePath(digest));
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete blob", e);
            }
        });
    }
}
