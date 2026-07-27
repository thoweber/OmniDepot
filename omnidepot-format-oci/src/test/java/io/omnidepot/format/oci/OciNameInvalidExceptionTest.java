package io.omnidepot.format.oci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OciNameInvalidExceptionTest {

    @Test
    @DisplayName("Given constructors - when OciNameInvalidException is instantiated - then OCI error code and detail are preserved")
    void shouldConstructOciNameInvalidException() {
        var ex1 = new OciNameInvalidException("Invalid name");
        var ex2 = new OciNameInvalidException("Invalid name", "Detail object");

        assertThat(ex1.getOciErrorCode()).isEqualTo("NAME_INVALID");
        assertThat(ex1.getDetail()).isEmpty();

        assertThat(ex2.getOciErrorCode()).isEqualTo("NAME_INVALID");
        assertThat(ex2.getDetail()).contains("Detail object");
    }
}
