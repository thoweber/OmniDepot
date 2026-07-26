package io.omnidepot.format.oci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OciDigestInvalidExceptionTest {

    @Test
    @DisplayName("Given constructors - when OciDigestInvalidException is instantiated - then OCI error code, cause, and detail are preserved")
    void shouldConstructOciDigestInvalidException() {
        var cause = new RuntimeException("Format error");
        var ex1 = new OciDigestInvalidException("Invalid digest");
        var ex2 = new OciDigestInvalidException("Invalid digest with cause", cause);
        var ex3 = new OciDigestInvalidException("Invalid digest with detail", "Detail object");

        assertThat(ex1.getOciErrorCode()).isEqualTo("DIGEST_INVALID");
        assertThat(ex1.getDetail()).isEmpty();

        assertThat(ex2.getOciErrorCode()).isEqualTo("DIGEST_INVALID");
        assertThat(ex2.getCause()).isSameAs(cause);

        assertThat(ex3.getOciErrorCode()).isEqualTo("DIGEST_INVALID");
        assertThat(ex3.getDetail()).contains("Detail object");
    }
}
