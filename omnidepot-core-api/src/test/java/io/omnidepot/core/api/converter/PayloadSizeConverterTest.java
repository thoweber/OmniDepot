package io.omnidepot.core.api.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadSizeConverterTest {

    @Test
    @DisplayName("Given null payload, formatPayload returns null string")
    void shouldFormatNullPayload() {
        assertThat(PayloadSizeConverter.formatPayload(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("Given small payload under 1KB, formatPayload returns byte[](X B)")
    void shouldFormatBytesPayload() {
        byte[] payload = new byte[500];
        assertThat(PayloadSizeConverter.formatPayload(payload)).isEqualTo("byte[](500B)");
        assertThat(PayloadSizeConverter.formatSize(500)).isEqualTo("byte[](500B)");
    }

    @Test
    @DisplayName("Given payload in kilobytes, formatPayload returns byte[](X.X KB)")
    void shouldFormatKilobytesPayload() {
        byte[] payload = new byte[2048];
        assertThat(PayloadSizeConverter.formatPayload(payload)).isEqualTo("byte[](2.0KB)");
        assertThat(PayloadSizeConverter.formatSize(2048)).isEqualTo("byte[](2.0KB)");
    }

    @Test
    @DisplayName("Given payload in megabytes, formatPayload returns byte[](X.X MB)")
    void shouldFormatMegabytesPayload() {
        byte[] payload = new byte[1_258_291]; // ~1.2 MB
        assertThat(PayloadSizeConverter.formatPayload(payload)).isEqualTo("byte[](1.2MB)");
        assertThat(PayloadSizeConverter.formatSize(1_258_291)).isEqualTo("byte[](1.2MB)");
    }
}
