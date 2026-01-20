package com.bookcover.archive.validation;

import org.springframework.stereotype.Component;

/**
 * Validator for ISBN-10 and ISBN-13 formats.
 * Validates: Requirements 1.2, 2.3
 */
@Component
public class IsbnValidator {

    /**
     * Validates an ISBN string (either ISBN-10 or ISBN-13).
     *
     * @param isbn the ISBN string to validate
     * @return true if the ISBN is valid, false otherwise
     */
    public boolean isValid(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return false;
        }

        String normalized = normalizeIsbn(isbn);

        return isValidIsbn10(normalized) || isValidIsbn13(normalized);
    }

    /**
     * Validates an ISBN-10 format.
     * ISBN-10 consists of 9 digits followed by a check digit (0-9 or X).
     *
     * @param isbn the ISBN string to validate
     * @return true if the ISBN-10 is valid, false otherwise
     */
    public boolean isValidIsbn10(String isbn) {
        if (isbn == null) {
            return false;
        }

        String normalized = normalizeIsbn(isbn);

        if (normalized.length() != 10) {
            return false;
        }

        // First 9 characters must be digits
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                return false;
            }
        }

        // Last character can be digit or 'X'
        char lastChar = normalized.charAt(9);
        if (!Character.isDigit(lastChar) && lastChar != 'X') {
            return false;
        }

        return calculateIsbn10CheckDigit(normalized) == lastChar;
    }

    /**
     * Validates an ISBN-13 format.
     * ISBN-13 consists of 13 digits with a specific check digit algorithm.
     *
     * @param isbn the ISBN string to validate
     * @return true if the ISBN-13 is valid, false otherwise
     */
    public boolean isValidIsbn13(String isbn) {
        if (isbn == null) {
            return false;
        }

        String normalized = normalizeIsbn(isbn);

        if (normalized.length() != 13) {
            return false;
        }

        // All characters must be digits
        for (int i = 0; i < 13; i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                return false;
            }
        }

        return calculateIsbn13CheckDigit(normalized) == normalized.charAt(12);
    }

    /**
     * Normalizes an ISBN by removing hyphens and spaces, and converting to uppercase.
     *
     * @param isbn the ISBN string to normalize
     * @return the normalized ISBN string
     */
    public String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s-]", "").toUpperCase();
    }

    /**
     * Calculates the check digit for ISBN-10.
     * The check digit is calculated as: (11 - (sum of (digit * weight) mod 11)) mod 11
     * where weight goes from 10 to 2 for positions 1 to 9.
     */
    private char calculateIsbn10CheckDigit(String isbn) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn.charAt(i) - '0') * (10 - i);
        }
        int checkDigit = (11 - (sum % 11)) % 11;
        return checkDigit == 10 ? 'X' : (char) ('0' + checkDigit);
    }

    /**
     * Calculates the check digit for ISBN-13.
     * The check digit is calculated as: (10 - (sum mod 10)) mod 10
     * where alternating weights of 1 and 3 are applied.
     */
    private char calculateIsbn13CheckDigit(String isbn) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return (char) ('0' + checkDigit);
    }
}
