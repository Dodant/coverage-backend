package com.bookcover.archive.validation

/**
 * ISBN Validator for ISBN-10 and ISBN-13 formats.
 *
 * ISBN-10: 10 digits where the last digit can be 'X' (representing 10)
 * ISBN-13: 13 digits starting with 978 or 979
 *
 * Requirements: 1.2, 2.3
 */
object IsbnValidator {

    data class ValidationResult(
        val valid: Boolean,
        val format: IsbnFormat? = null,
        val normalizedIsbn: String? = null,
        val errorMessage: String? = null
    )

    enum class IsbnFormat {
        ISBN_10,
        ISBN_13
    }

    /**
     * Validates an ISBN string and returns detailed validation result.
     */
    fun validate(isbn: String?): ValidationResult {
        if (isbn.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "ISBN cannot be null or blank")
        }

        val normalized = normalizeIsbn(isbn)

        return when (normalized.length) {
            10 -> validateIsbn10(normalized)
            13 -> validateIsbn13(normalized)
            else -> ValidationResult(
                valid = false,
                errorMessage = "ISBN must be 10 or 13 characters (excluding hyphens/spaces)"
            )
        }
    }

    /**
     * Checks if an ISBN is valid (simple boolean check).
     */
    fun isValid(isbn: String?): Boolean = validate(isbn).valid

    /**
     * Normalizes ISBN by removing hyphens and spaces, converting to uppercase.
     */
    fun normalizeIsbn(isbn: String): String {
        return isbn.replace("-", "")
            .replace(" ", "")
            .uppercase()
    }

    /**
     * Validates ISBN-10 format.
     *
     * ISBN-10 check digit calculation:
     * Sum of (digit * position) where position is 10 to 1
     * The sum must be divisible by 11
     * Last digit can be 'X' representing 10
     */
    private fun validateIsbn10(isbn: String): ValidationResult {
        if (isbn.length != 10) {
            return ValidationResult(valid = false, errorMessage = "ISBN-10 must be exactly 10 characters")
        }

        // First 9 characters must be digits
        val firstNine = isbn.substring(0, 9)
        if (!firstNine.all { it.isDigit() }) {
            return ValidationResult(valid = false, errorMessage = "First 9 characters of ISBN-10 must be digits")
        }

        // Last character can be digit or 'X'
        val lastChar = isbn[9]
        if (!lastChar.isDigit() && lastChar != 'X') {
            return ValidationResult(valid = false, errorMessage = "Last character of ISBN-10 must be a digit or 'X'")
        }

        // Calculate checksum
        var sum = 0
        for (i in 0 until 9) {
            sum += (isbn[i] - '0') * (10 - i)
        }

        val lastDigitValue = if (lastChar == 'X') 10 else (lastChar - '0')
        sum += lastDigitValue

        val isValid = sum % 11 == 0

        return if (isValid) {
            ValidationResult(
                valid = true,
                format = IsbnFormat.ISBN_10,
                normalizedIsbn = isbn
            )
        } else {
            ValidationResult(valid = false, errorMessage = "Invalid ISBN-10 checksum")
        }
    }

    /**
     * Validates ISBN-13 format.
     *
     * ISBN-13 check digit calculation:
     * Sum of (digit * weight) where weight alternates between 1 and 3
     * The sum must be divisible by 10
     */
    private fun validateIsbn13(isbn: String): ValidationResult {
        if (isbn.length != 13) {
            return ValidationResult(valid = false, errorMessage = "ISBN-13 must be exactly 13 characters")
        }

        // All characters must be digits
        if (!isbn.all { it.isDigit() }) {
            return ValidationResult(valid = false, errorMessage = "All characters of ISBN-13 must be digits")
        }

        // ISBN-13 must start with 978 or 979
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
            return ValidationResult(valid = false, errorMessage = "ISBN-13 must start with 978 or 979")
        }

        // Calculate checksum
        var sum = 0
        for (i in 0 until 13) {
            val digit = isbn[i] - '0'
            val weight = if (i % 2 == 0) 1 else 3
            sum += digit * weight
        }

        val isValid = sum % 10 == 0

        return if (isValid) {
            ValidationResult(
                valid = true,
                format = IsbnFormat.ISBN_13,
                normalizedIsbn = isbn
            )
        } else {
            ValidationResult(valid = false, errorMessage = "Invalid ISBN-13 checksum")
        }
    }

    /**
     * Converts ISBN-10 to ISBN-13.
     */
    fun convertIsbn10ToIsbn13(isbn10: String): String? {
        val normalized = normalizeIsbn(isbn10)
        if (normalized.length != 10 || !validateIsbn10(normalized).valid) {
            return null
        }

        // Remove check digit and prepend 978
        val isbn13Base = "978" + normalized.substring(0, 9)

        // Calculate new check digit
        var sum = 0
        for (i in 0 until 12) {
            val digit = isbn13Base[i] - '0'
            val weight = if (i % 2 == 0) 1 else 3
            sum += digit * weight
        }

        val checkDigit = (10 - (sum % 10)) % 10
        return isbn13Base + checkDigit
    }
}
