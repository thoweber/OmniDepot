package io.omnidepot.format.oci.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a string is a non-blank, valid OCI SHA-256 digest format (ADR-004, ADR-028).
 */
@Documented
@Constraint(validatedBy = OciDigestValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOciDigest {

    String message() default "Invalid OCI digest format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
