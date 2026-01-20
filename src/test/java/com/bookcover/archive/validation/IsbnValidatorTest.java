package com.bookcover.archive.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IsbnValidator.
 * Validates: Requirements 1.2, 2.3
 */
class IsbnValidatorTest {

    private IsbnValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IsbnValidator();
    }

    @Nested
    @DisplayName("ISBN-10 Validation")
    class Isbn10Tests {

        @Test
        @DisplayName("Valid ISBN-10 with numeric check digit")
        void validIsbn10WithNumericCheckDigit() {
            assertTrue(validator.isValidIsbn10("0306406152"));
        }

        @Test
        @DisplayName("Valid ISBN-10 with X check digit")
        void validIsbn10WithXCheckDigit() {
            assertTrue(validator.isValidIsbn10("080442957X"));
        }

        @Test
        @DisplayName("Valid ISBN-10 with hyphens")
        void validIsbn10WithHyphens() {
            assertTrue(validator.isValidIsbn10("0-306-40615-2"));
        }

        @Test
        @DisplayName("Invalid ISBN-10 with wrong check digit")
        void invalidIsbn10WrongCheckDigit() {
            assertFalse(validator.isValidIsbn10("0306406153"));
        }

        @Test
        @DisplayName("Invalid ISBN-10 with wrong length")
        void invalidIsbn10WrongLength() {
            assertFalse(validator.isValidIsbn10("030640615"));
        }

        @Test
        @DisplayName("Invalid ISBN-10 with non-digit characters")
        void invalidIsbn10NonDigit() {
            assertFalse(validator.isValidIsbn10("030640615A"));
        }
    }

    @Nested
    @DisplayName("ISBN-13 Validation")
    class Isbn13Tests {

        @Test
        @DisplayName("Valid ISBN-13")
        void validIsbn13() {
            assertTrue(validator.isValidIsbn13("9780306406157"));
        }

        @Test
        @DisplayName("Valid ISBN-13 with hyphens")
        void validIsbn13WithHyphens() {
            assertTrue(validator.isValidIsbn13("978-0-306-40615-7"));
        }

        @Test
        @DisplayName("Invalid ISBN-13 with wrong check digit")
        void invalidIsbn13WrongCheckDigit() {
            assertFalse(validator.isValidIsbn13("9780306406158"));
        }

        @Test
        @DisplayName("Invalid ISBN-13 with wrong length")
        void invalidIsbn13WrongLength() {
            assertFalse(validator.isValidIsbn13("978030640615"));
        }

        @Test
        @DisplayName("Invalid ISBN-13 with non-digit characters")
        void invalidIsbn13NonDigit() {
            assertFalse(validator.isValidIsbn13("978030640615X"));
        }
    }

    @Nested
    @DisplayName("General ISBN Validation")
    class GeneralTests {

        @Test
        @DisplayName("Null ISBN is invalid")
        void nullIsbnInvalid() {
            assertFalse(validator.isValid(null));
        }

        @Test
        @DisplayName("Empty ISBN is invalid")
        void emptyIsbnInvalid() {
            assertFalse(validator.isValid(""));
        }

        @Test
        @DisplayName("Blank ISBN is invalid")
        void blankIsbnInvalid() {
            assertFalse(validator.isValid("   "));
        }

        @Test
        @DisplayName("Valid ISBN-10 passes general validation")
        void validIsbn10PassesGeneral() {
            assertTrue(validator.isValid("0306406152"));
        }

        @Test
        @DisplayName("Valid ISBN-13 passes general validation")
        void validIsbn13PassesGeneral() {
            assertTrue(validator.isValid("9780306406157"));
        }
    }

    @Nested
    @DisplayName("ISBN Normalization")
    class NormalizationTests {

        @Test
        @DisplayName("Removes hyphens")
        void removesHyphens() {
            assertEquals("9780306406157", validator.normalizeIsbn("978-0-306-40615-7"));
        }

        @Test
        @DisplayName("Removes spaces")
        void removesSpaces() {
            assertEquals("9780306406157", validator.normalizeIsbn("978 0 306 40615 7"));
        }

        @Test
        @DisplayName("Converts to uppercase")
        void convertsToUppercase() {
            assertEquals("080442957X", validator.normalizeIsbn("080442957x"));
        }

        @Test
        @DisplayName("Null returns null")
        void nullReturnsNull() {
            assertNull(validator.normalizeIsbn(null));
        }
    }
}
