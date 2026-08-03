package io.omnidepot.format.maven;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;

/**
 * Value record representing parsed Maven GAV coordinates and layout information.
 */
@Builder(toBuilder = true)
public record MavenCoordinates(
        String groupId,
        String artifactId,
        String version,
        String filename,
        boolean isSnapshot,
        boolean isChecksumRequest,
        @Nullable String checksumExtension,
        @Nullable String checksumAlgorithm,
        String primaryPath
) {
    private static final Set<String> CHECKSUM_EXTENSIONS = Set.of("sha256", "sha1", "md5", "sha512");

    public static MavenCoordinates parse(@Nullable String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be null or blank");
        }

        String path = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;

        String primaryPath = path;
        boolean isChecksum = false;
        String checksumExt = null;
        String checksumAlg = null;

        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1) {
            String ext = path.substring(lastDot + 1).toLowerCase();
            if (CHECKSUM_EXTENSIONS.contains(ext)) {
                isChecksum = true;
                checksumExt = ext;
                checksumAlg = mapAlgorithm(ext);
                primaryPath = path.substring(0, lastDot);
            }
        }

        String[] parts = primaryPath.split("/");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid Maven GAV layout path: " + rawPath);
        }

        String filename = parts[parts.length - 1];
        String version = parts[parts.length - 2];
        String artifactId = parts[parts.length - 3];
        String groupId = String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 3));

        boolean isSnapshot = version.toUpperCase().contains("SNAPSHOT");

        return MavenCoordinates.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .filename(filename)
                .isSnapshot(isSnapshot)
                .isChecksumRequest(isChecksum)
                .checksumExtension(checksumExt)
                .checksumAlgorithm(checksumAlg)
                .primaryPath(primaryPath)
                .build();
    }

    private static String mapAlgorithm(String ext) {
        return switch (ext) {
            case "sha256" -> "SHA-256";
            case "sha1" -> "SHA-1"; // NOSONAR legacy Maven checksum algorithm
            case "md5" -> "MD5";   // NOSONAR legacy Maven checksum algorithm
            case "sha512" -> "SHA-512";
            default -> throw new IllegalArgumentException("Unsupported checksum algorithm extension: " + ext);
        };
    }

    public static String computeChecksum(byte[] data, @Nullable String extOrAlgorithm) {
        if (extOrAlgorithm == null) {
            throw new IllegalArgumentException("Checksum algorithm must not be null");
        }
        String algo = CHECKSUM_EXTENSIONS.contains(extOrAlgorithm.toLowerCase())
                ? mapAlgorithm(extOrAlgorithm.toLowerCase())
                : extOrAlgorithm;
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported digest algorithm: " + algo, e);
        }
    }
}
