package com.bookcover.archive.validation.constraint;

import com.bookcover.archive.validation.CountryCodeValidator;
import com.bookcover.archive.validation.annotation.ValidCountryCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for the @ValidCountryCode annotation.
 *
 * Validates: Requirements 13.1
 */
public class CountryCodeConstraintValidator implements ConstraintValidator<ValidCountryCode, String> {

    private final CountryCodeValidator countryCodeValidator = new CountryCodeValidator();
    private boolean optional;

    @Override
    public void initialize(ValidCountryCode constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return countryCodeValidator.isValid(value);
    }
}
