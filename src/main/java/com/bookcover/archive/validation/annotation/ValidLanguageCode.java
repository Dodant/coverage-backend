package com.bookcover.archive.validation.annotation;

import com.bookcover.archive.validation.constraint.LanguageCodeConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for ISO 639-1 language codes.
 *
 * Validates: Requirements 13.2
 */
@Documented
@Constraint(validatedBy = LanguageCodeConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLanguageCode {

    String message() default "Invalid ISO 639-1 language code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null/empty values.
     */
    boolean optional() default false;
}
