package com.bookcover.archive.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LanguageCodeValidator.
 * Validates: Requirements 13.2
 */
class LanguageCodeValidatorTest {

    private LanguageCodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LanguageCodeValidator();
    }

    @Test
    @DisplayName("Valid language code ko")
    void validLanguageCodeKo() {
        assertTrue(validator.isValid("ko"));
    }

    @Test
    @DisplayName("Valid language code en")
    void validLanguageCodeEn() {
        assertTrue(validator.isValid("en"));
    }

    @Test
    @DisplayName("Valid language code ja")
    void validLanguageCodeJa() {
        assertTrue(validator.isValid("ja"));
    }

    @Test
    @DisplayName("Valid uppercase language code")
    void validUppercaseLanguageCode() {
        assertTrue(validator.isValid("KO"));
    }

    @Test
    @DisplayName("Invalid language code")
    void invalidLanguageCode() {
        assertFalse(validator.isValid("xx"));
    }

    @Test
    @DisplayName("Null language code is invalid")
    void nullLanguageCodeInvalid() {
        assertFalse(validator.isValid(null));
    }

    @Test
    @DisplayName("Empty language code is invalid")
    void emptyLanguageCodeInvalid() {
        assertFalse(validator.isValid(""));
    }

    @Test
    @DisplayName("Blank language code is invalid")
    void blankLanguageCodeInvalid() {
        assertFalse(validator.isValid("   "));
    }

    @Test
    @DisplayName("Three letter code is invalid")
    void threeLetterCodeInvalid() {
        assertFalse(validator.isValid("kor"));
    }

    @Test
    @DisplayName("Normalize converts to lowercase")
    void normalizeConvertsToLowercase() {
        assertEquals("ko", validator.normalize("KO"));
    }

    @Test
    @DisplayName("Normalize trims whitespace")
    void normalizeTrimsWhitespace() {
        assertEquals("ko", validator.normalize("  ko  "));
    }

    @Test
    @DisplayName("Normalize null returns null")
    void normalizeNullReturnsNull() {
        assertNull(validator.normalize(null));
    }

    @Test
    @DisplayName("Valid language codes set is not empty")
    void validLanguageCodesNotEmpty() {
        assertFalse(validator.getValidLanguageCodes().isEmpty());
    }
}
