package io.omnidepot.format.oci.validation;

import io.omnidepot.format.oci.OciDigest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.isNull;

/**
 * Jakarta ConstraintValidator implementation for @ValidOciDigest (ADR-004, ADR-028).
 */
public class OciDigestValidator implements ConstraintValidator<ValidOciDigest, String> {

    @Override
    @SuppressWarnings({"java:S1166", "java:S7467"})
    public boolean isValid(@Nullable String value, @Nullable ConstraintValidatorContext context) {
        if (isNull(value) || value.isBlank()) {
            return false;
        }
        try {
            OciDigest.of(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
