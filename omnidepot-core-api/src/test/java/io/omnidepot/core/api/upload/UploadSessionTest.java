package io.omnidepot.core.api.upload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadSessionTest {

    @Test
    @DisplayName("Given valid parameters - when UploadSession is created - then fields are correctly populated")
    void shouldCreateUploadSessionWithAllFields() {
        Instant now = Instant.now();
        byte[] partialState = new byte[]{1, 2, 3, 4};
        UploadSession session = new UploadSession("id-1", "repo-1", "token-1", 100L, 500L, UploadSessionStatus.INITIATED, "{}", partialState, now, now);

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
    @DisplayName("Given null mandatory parameters - when constructor is invoked - then NullPointerException is thrown")
    void shouldValidateNullConstructorArguments() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new UploadSession(null, "repo-1", "token-1", 0L, null, UploadSessionStatus.INITIATED, "{}", null, now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UploadSession("id-1", null, "token-1", 0L, null, UploadSessionStatus.INITIATED, "{}", null, now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UploadSession("id-1", "repo-1", null, 0L, null, UploadSessionStatus.INITIATED, "{}", null, now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UploadSession("id-1", "repo-1", "token-1", 0L, null, null, "{}", null, now, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UploadSession("id-1", "repo-1", "token-1", 0L, null, UploadSessionStatus.INITIATED, "{}", null, null, now))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UploadSession("id-1", "repo-1", "token-1", 0L, null, UploadSessionStatus.INITIATED, "{}", null, now, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Given two identical UploadSessions with array content - when equals and hashCode are evaluated - then they match")
    void shouldImplementEqualsAndHashCodeWithArrayContent() {
        Instant now = Instant.now();
        byte[] state1 = new byte[]{10, 20, 30};
        byte[] state2 = new byte[]{10, 20, 30};

        UploadSession session1 = new UploadSession("id-1", "repo-1", "token-1", 100L, 500L, UploadSessionStatus.INITIATED, "{}", state1, now, now);
        UploadSession session2 = new UploadSession("id-1", "repo-1", "token-1", 100L, 500L, UploadSessionStatus.INITIATED, "{}", state2, now, now);
        UploadSession diffSession = new UploadSession("id-2", "repo-1", "token-1", 100L, 500L, UploadSessionStatus.INITIATED, "{}", state1, now, now);

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
