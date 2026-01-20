package com.bookcover.archive.validation.constraint;

import com.bookcover.archive.validation.LanguageCodeValidator;
import com.bookcover.archive.validation.annotation.ValidLanguageCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for the @ValidLanguageCode annotation.
 *
 * Validates: Requirements 13.2
 */
public class LanguageCodeConstraintValidator implements ConstraintValidator<ValidLanguageCode, String> {

    private final LanguageCodeValidator languageCodeValidator = new LanguageCodeValidator();
    private boolean optional;

    @Override
    public void initialize(ValidLanguageCode constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return languageCodeValidator.isValid(value);
    }
}
