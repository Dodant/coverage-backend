package com.bookcover.archive.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UrlValidator.
 * Validates: Requirements 7.2, 7.3
 */
class UrlValidatorTest {

    private UrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrlValidator();
    }

    @Nested
    @DisplayName("Valid URLs")
    class ValidUrlTests {

        @Test
        @DisplayName("HTTPS URL is valid")
        void httpsUrlValid() {
            assertTrue(validator.isValid("https://example.com"));
        }

        @Test
        @DisplayName("HTTP URL is valid")
        void httpUrlValid() {
            assertTrue(validator.isValid("http://example.com"));
        }

        @Test
        @DisplayName("URL with path is valid")
        void urlWithPathValid() {
            assertTrue(validator.isValid("https://example.com/path/to/page"));
        }

        @Test
        @DisplayName("URL with query parameters is valid")
        void urlWithQueryValid() {
            assertTrue(validator.isValid("https://example.com/page?param=value"));
        }

        @Test
        @DisplayName("URL with subdomain is valid")
        void urlWithSubdomainValid() {
            assertTrue(validator.isValid("https://www.example.com"));
        }

        @Test
        @DisplayName("URL with port is valid")
        void urlWithPortValid() {
            assertTrue(validator.isValid("https://example.com:8080"));
        }
    }

    @Nested
    @DisplayName("Invalid URLs")
    class InvalidUrlTests {

        @Test
        @DisplayName("Null URL is invalid")
        void nullUrlInvalid() {
            assertFalse(validator.isValid(null));
        }

        @Test
        @DisplayName("Empty URL is invalid")
        void emptyUrlInvalid() {
            assertFalse(validator.isValid(""));
        }

        @Test
        @DisplayName("Blank URL is invalid")
        void blankUrlInvalid() {
            assertFalse(validator.isValid("   "));
        }

        @Test
        @DisplayName("URL without scheme is invalid")
        void urlWithoutSchemeInvalid() {
            assertFalse(validator.isValid("example.com"));
        }

        @Test
        @DisplayName("FTP URL is invalid")
        void ftpUrlInvalid() {
            assertFalse(validator.isValid("ftp://example.com"));
        }

        @Test
        @DisplayName("File URL is invalid")
        void fileUrlInvalid() {
            assertFalse(validator.isValid("file:///path/to/file"));
        }

        @Test
        @DisplayName("Malformed URL is invalid")
        void malformedUrlInvalid() {
            assertFalse(validator.isValid("https://"));
        }

        @Test
        @DisplayName("URL with spaces is invalid")
        void urlWithSpacesInvalid() {
            assertFalse(validator.isValid("https://example .com"));
        }
    }

    @Nested
    @DisplayName("Website URL Validation")
    class WebsiteUrlTests {

        @Test
        @DisplayName("Valid website URL")
        void validWebsiteUrl() {
            assertTrue(validator.isValidWebsiteUrl("https://mywebsite.com"));
        }

        @Test
        @DisplayName("Invalid website URL")
        void invalidWebsiteUrl() {
            assertFalse(validator.isValidWebsiteUrl("not-a-url"));
        }
    }

    @Nested
    @DisplayName("Portfolio URL Validation")
    class PortfolioUrlTests {

        @Test
        @DisplayName("Valid portfolio URL")
        void validPortfolioUrl() {
            assertTrue(validator.isValidPortfolioUrl("https://behance.net/myportfolio"));
        }

        @Test
        @DisplayName("Invalid portfolio URL")
        void invalidPortfolioUrl() {
            assertFalse(validator.isValidPortfolioUrl("invalid"));
        }
    }

    @Nested
    @DisplayName("URL Normalization")
    class NormalizationTests {

        @Test
        @DisplayName("Trims whitespace")
        void trimsWhitespace() {
            assertEquals("https://example.com", validator.normalizeUrl("  https://example.com  "));
        }

        @Test
        @DisplayName("Null returns null")
        void nullReturnsNull() {
            assertNull(validator.normalizeUrl(null));
        }
    }
}
