package com.bookcover.archive.validation.constraint;

import com.bookcover.archive.validation.IsbnValidator;
import com.bookcover.archive.validation.annotation.ValidIsbn;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for the @ValidIsbn annotation.
 *
 * Validates: Requirements 1.2, 2.3
 */
public class IsbnConstraintValidator implements ConstraintValidator<ValidIsbn, String> {

    private final IsbnValidator isbnValidator = new IsbnValidator();
    private boolean optional;

    @Override
    public void initialize(ValidIsbn constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return isbnValidator.isValid(value);
    }
}
