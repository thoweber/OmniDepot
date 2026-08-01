package io.omnidepot.core.api.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadSessionTest {

    @Test
    @DisplayName("Given valid parameters - when UploadSession is created via builder - then fields are correctly populated")
    void shouldCreateUploadSessionWithAllFields() {
        Instant now = Instant.now();
        byte[] partialState = new byte[]{1, 2, 3, 4};
        UploadSession session = UploadSession.builder()
                .id("id-1")
                .repositoryId("repo-1")
                .uploadToken("token-1")
                .bytesReceived(100L)
                .totalBytes(500L)
                .status(UploadSessionStatus.INITIATED)
                .providerStateJson("{}")
                .sha256PartialState(partialState)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(session.id()).isEqualTo("id-1");
        assertThat(session.repositoryId()).isEqualTo("repo-1");
        assertThat(session.uploadToken()).isEqualTo("token-1");
        assertThat(session.bytesReceived()).isEqualTo(100L);
        assertThat(session.totalBytes()).isEqualTo(500L);
        assertThat(session.status()).isEqualTo(UploadSessionStatus.INITIATED);
        assertThat(session.providerStateJson()).isEqualTo("{}");
        assertThat(session.sha256PartialState()).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("Given existing session - when toBuilder is invoked - then modified copy is produced cleanly")
    void shouldSupportToBuilderModification() {
        Instant now = Instant.now();
        UploadSession initial = UploadSession.builder()
                .id("id-1")
                .repositoryId("repo-1")
                .uploadToken("token-1")
                .bytesReceived(100L)
                .totalBytes(500L)
                .status(UploadSessionStatus.INITIATED)
                .providerStateJson("{}")
                .createdAt(now)
                .updatedAt(now)
                .build();

        UploadSession updated = initial.toBuilder()
                .bytesReceived(250L)
                .status(UploadSessionStatus.COMPLETED)
                .build();

        assertThat(updated.id()).isEqualTo("id-1");
        assertThat(updated.bytesReceived()).isEqualTo(250L);
        assertThat(updated.status()).isEqualTo(UploadSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Given null mandatory parameters - when builder build is invoked - then NullPointerException is thrown")
    void shouldValidateNullConstructorArguments() {
        Instant now = Instant.now();

        UploadSession.UploadSessionBuilder b1 = UploadSession.builder().id(null).repositoryId("repo-1").uploadToken("token-1").bytesReceived(0L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").createdAt(now).updatedAt(now);
        assertThatThrownBy(b1::build).isInstanceOf(NullPointerException.class);

        UploadSession.UploadSessionBuilder b2 = UploadSession.builder().id("id-1").repositoryId(null).uploadToken("token-1").bytesReceived(0L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").createdAt(now).updatedAt(now);
        assertThatThrownBy(b2::build).isInstanceOf(NullPointerException.class);

        UploadSession.UploadSessionBuilder b3 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken(null).bytesReceived(0L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").createdAt(now).updatedAt(now);
        assertThatThrownBy(b3::build).isInstanceOf(NullPointerException.class);

        UploadSession.UploadSessionBuilder b4 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken("token-1").bytesReceived(0L).status(null).providerStateJson("{}").createdAt(now).updatedAt(now);
        assertThatThrownBy(b4::build).isInstanceOf(NullPointerException.class);

        UploadSession.UploadSessionBuilder b5 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken("token-1").bytesReceived(0L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").createdAt(null).updatedAt(now);
        assertThatThrownBy(b5::build).isInstanceOf(NullPointerException.class);

        UploadSession.UploadSessionBuilder b6 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken("token-1").bytesReceived(0L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").createdAt(now).updatedAt(null);
        assertThatThrownBy(b6::build).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Given two identical UploadSessions with array content - when equals and hashCode are evaluated - then they match")
    void shouldImplementEqualsAndHashCodeWithArrayContent() {
        Instant now = Instant.now();
        byte[] state1 = new byte[]{10, 20, 30};
        byte[] state2 = new byte[]{10, 20, 30};

        UploadSession session1 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken("token-1").bytesReceived(100L).totalBytes(500L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").sha256PartialState(state1).createdAt(now).updatedAt(now).build();
        UploadSession session2 = UploadSession.builder().id("id-1").repositoryId("repo-1").uploadToken("token-1").bytesReceived(100L).totalBytes(500L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").sha256PartialState(state2).createdAt(now).updatedAt(now).build();
        UploadSession diffSession = UploadSession.builder().id("id-2").repositoryId("repo-1").uploadToken("token-1").bytesReceived(100L).totalBytes(500L).status(UploadSessionStatus.INITIATED).providerStateJson("{}").sha256PartialState(state1).createdAt(now).updatedAt(now).build();

        assertThat(session1)
                .isEqualTo(session2)
                .hasSameHashCodeAs(session2)
                .isNotEqualTo(diffSession)
                .isNotEqualTo(null)
                .isNotEqualTo("string");
        assertThat(session1.toString())
                .contains("id-1")
                .contains("sha256PartialState=[10, 20, 30]");
    }
}
