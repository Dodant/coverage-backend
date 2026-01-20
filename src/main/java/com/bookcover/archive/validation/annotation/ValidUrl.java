package com.bookcover.archive.validation.annotation;

import com.bookcover.archive.validation.constraint.UrlConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for URL fields.
 * Validates HTTP/HTTPS URLs.
 *
 * Validates: Requirements 7.2, 7.3
 */
@Documented
@Constraint(validatedBy = UrlConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUrl {

    String message() default "Invalid URL format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null/empty values.
     * If true, null or empty strings will pass validation.
     */
    boolean optional() default true;
}
