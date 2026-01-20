package com.bookcover.archive.validation.annotation;

import com.bookcover.archive.validation.constraint.IsbnConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for ISBN fields.
 * Validates both ISBN-10 and ISBN-13 formats.
 *
 * Validates: Requirements 1.2, 2.3
 */
@Documented
@Constraint(validatedBy = IsbnConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIsbn {

    String message() default "Invalid ISBN format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null/empty values.
     * If true, null or empty strings will pass validation.
     */
    boolean optional() default true;
}
