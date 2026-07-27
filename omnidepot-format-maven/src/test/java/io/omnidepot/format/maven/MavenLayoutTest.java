package io.omnidepot.format.maven;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenLayoutTest {

    @Test
    @DisplayName("Given a valid release JAR GAV path, parse Maven coordinates correctly")
    void shouldParseReleaseJarPath() {
        String path = "io/omnidepot/core/my-app/1.2.3/my-app-1.2.3.jar";

        MavenCoordinates coords = MavenCoordinates.parse(path);

        assertThat(coords.groupId()).isEqualTo("io.omnidepot.core");
        assertThat(coords.artifactId()).isEqualTo("my-app");
        assertThat(coords.version()).isEqualTo("1.2.3");
        assertThat(coords.filename()).isEqualTo("my-app-1.2.3.jar");
        assertThat(coords.isSnapshot()).isFalse();
        assertThat(coords.isChecksumRequest()).isFalse();
        assertThat(coords.checksumAlgorithm()).isNull();
        assertThat(coords.primaryPath()).isEqualTo("io/omnidepot/core/my-app/1.2.3/my-app-1.2.3.jar");
    }

    @Test
    @DisplayName("Given a path starting with leading slash, parse correctly by stripping slash")
    void shouldParsePathWithLeadingSlash() {
        String path = "/io/omnidepot/core/my-app/1.2.3/my-app-1.2.3.jar";

        MavenCoordinates coords = MavenCoordinates.parse(path);

        assertThat(coords.groupId()).isEqualTo("io.omnidepot.core");
        assertThat(coords.artifactId()).isEqualTo("my-app");
    }

    @Test
    @DisplayName("Given a valid SNAPSHOT POM GAV path, identify snapshot correctly")
    void shouldParseSnapshotPomPath() {
        String path = "com/example/widget/2.0.0-SNAPSHOT/widget-2.0.0-SNAPSHOT.pom";

        MavenCoordinates coords = MavenCoordinates.parse(path);

        assertThat(coords.groupId()).isEqualTo("com.example");
        assertThat(coords.artifactId()).isEqualTo("widget");
        assertThat(coords.version()).isEqualTo("2.0.0-SNAPSHOT");
        assertThat(coords.filename()).isEqualTo("widget-2.0.0-SNAPSHOT.pom");
        assertThat(coords.isSnapshot()).isTrue();
        assertThat(coords.isChecksumRequest()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sha256", "sha1", "md5", "sha512"})
    @DisplayName("Given checksum file requests, parse primary artifact path and target algorithm")
    void shouldParseChecksumFileRequests(String ext) {
        String path = "org/acme/lib/0.1.0/lib-0.1.0.jar." + ext;

        MavenCoordinates coords = MavenCoordinates.parse(path);

        assertThat(coords.isChecksumRequest()).isTrue();
        assertThat(coords.checksumExtension()).isEqualTo(ext);
        assertThat(coords.primaryPath()).isEqualTo("org/acme/lib/0.1.0/lib-0.1.0.jar");
        assertThat(coords.groupId()).isEqualTo("org.acme");
        assertThat(coords.artifactId()).isEqualTo("lib");
        assertThat(coords.version()).isEqualTo("0.1.0");
    }

    @Test
    @DisplayName("Given byte content, compute sha256, sha1, md5, sha512 hashes correctly")
    void shouldComputeChecksumHashes() {
        byte[] content = "hello omnidepot".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String sha256 = MavenCoordinates.computeChecksum(content, "sha256");
        String sha1 = MavenCoordinates.computeChecksum(content, "sha1");
        String md5 = MavenCoordinates.computeChecksum(content, "md5");
        String sha512 = MavenCoordinates.computeChecksum(content, "sha512");

        assertThat(sha256).hasSize(64);
        assertThat(sha1).hasSize(40);
        assertThat(md5).hasSize(32);
        assertThat(sha512).hasSize(128);
    }

    @Test
    @DisplayName("Given null, blank, or malformed paths, throw IllegalArgumentException")
    void shouldThrowOnInvalidPaths() {
        assertThatThrownBy(() -> MavenCoordinates.parse(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MavenCoordinates.parse("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> MavenCoordinates.parse("invalid-path.jar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given an unsupported algorithm, computeChecksum throws IllegalArgumentException")
    void shouldThrowOnUnsupportedAlgorithm() {
        byte[] content = "test".getBytes();
        assertThatThrownBy(() -> MavenCoordinates.computeChecksum(content, "INVALID_ALGO"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
