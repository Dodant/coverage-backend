package com.bookcover.archive.validation.annotation;

import com.bookcover.archive.validation.constraint.CountryCodeConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for ISO 3166-1 alpha-2 country codes.
 *
 * Validates: Requirements 13.1
 */
@Documented
@Constraint(validatedBy = CountryCodeConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCountryCode {

    String message() default "Invalid ISO 3166-1 country code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null/empty values.
     */
    boolean optional() default false;
}
