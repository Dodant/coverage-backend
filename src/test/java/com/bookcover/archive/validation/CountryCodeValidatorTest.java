package com.bookcover.archive.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CountryCodeValidator.
 * Validates: Requirements 13.1
 */
class CountryCodeValidatorTest {

    private CountryCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CountryCodeValidator();
    }

    @Test
    @DisplayName("Valid country code KR")
    void validCountryCodeKR() {
        assertTrue(validator.isValid("KR"));
    }

    @Test
    @DisplayName("Valid country code US")
    void validCountryCodeUS() {
        assertTrue(validator.isValid("US"));
    }

    @Test
    @DisplayName("Valid country code JP")
    void validCountryCodeJP() {
        assertTrue(validator.isValid("JP"));
    }

    @Test
    @DisplayName("Valid lowercase country code")
    void validLowercaseCountryCode() {
        assertTrue(validator.isValid("kr"));
    }

    @Test
    @DisplayName("Invalid country code")
    void invalidCountryCode() {
        assertFalse(validator.isValid("XX"));
    }

    @Test
    @DisplayName("Null country code is invalid")
    void nullCountryCodeInvalid() {
        assertFalse(validator.isValid(null));
    }

    @Test
    @DisplayName("Empty country code is invalid")
    void emptyCountryCodeInvalid() {
        assertFalse(validator.isValid(""));
    }

    @Test
    @DisplayName("Blank country code is invalid")
    void blankCountryCodeInvalid() {
        assertFalse(validator.isValid("   "));
    }

    @Test
    @DisplayName("Three letter code is invalid")
    void threeLetterCodeInvalid() {
        assertFalse(validator.isValid("KOR"));
    }

    @Test
    @DisplayName("Normalize converts to uppercase")
    void normalizeConvertsToUppercase() {
        assertEquals("KR", validator.normalize("kr"));
    }

    @Test
    @DisplayName("Normalize trims whitespace")
    void normalizeTrimsWhitespace() {
        assertEquals("KR", validator.normalize("  kr  "));
    }

    @Test
    @DisplayName("Normalize null returns null")
    void normalizeNullReturnsNull() {
        assertNull(validator.normalize(null));
    }

    @Test
    @DisplayName("Valid country codes set is not empty")
    void validCountryCodesNotEmpty() {
        assertFalse(validator.getValidCountryCodes().isEmpty());
    }
}
