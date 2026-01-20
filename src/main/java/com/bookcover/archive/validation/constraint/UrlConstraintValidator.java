package com.bookcover.archive.validation.constraint;

import com.bookcover.archive.validation.UrlValidator;
import com.bookcover.archive.validation.annotation.ValidUrl;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for the @ValidUrl annotation.
 *
 * Validates: Requirements 7.2, 7.3
 */
public class UrlConstraintValidator implements ConstraintValidator<ValidUrl, String> {

    private final UrlValidator urlValidator = new UrlValidator();
    private boolean optional;

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return urlValidator.isValid(value);
    }
}
