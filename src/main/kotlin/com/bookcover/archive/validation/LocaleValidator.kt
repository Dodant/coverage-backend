package com.bookcover.archive.validation

import java.util.Locale

/**
 * Locale Validator for country and language codes.
 *
 * Validates:
 * - ISO 3166-1 alpha-2 country codes (e.g., KR, US, JP)
 * - ISO 639-1 language codes (e.g., ko, en, ja)
 *
 * Requirements: 13.1, 13.2
 */
object LocaleValidator {

    // ISO 3166-1 alpha-2 country codes
    private val VALID_COUNTRY_CODES: Set<String> = Locale.getISOCountries().toSet()

    // ISO 639-1 language codes
    private val VALID_LANGUAGE_CODES: Set<String> = Locale.getISOLanguages().toSet()

    // Common country code mappings for display names
    private val COUNTRY_NAMES: Map<String, String> by lazy {
        VALID_COUNTRY_CODES.associateWith { code ->
            Locale("", code).displayCountry
        }
    }

    // Common language code mappings for display names
    private val LANGUAGE_NAMES: Map<String, String> by lazy {
        VALID_LANGUAGE_CODES.associateWith { code ->
            Locale(code).displayLanguage
        }
    }

    data class ValidationResult(
        val valid: Boolean,
        val normalizedCode: String? = null,
        val displayName: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Validates an ISO 3166-1 alpha-2 country code.
     */
    fun validateCountryCode(code: String?): ValidationResult {
        if (code.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "Country code cannot be null or blank")
        }

        val normalized = code.trim().uppercase()

        if (normalized.length != 2) {
            return ValidationResult(
                valid = false,
                errorMessage = "Country code must be exactly 2 characters (ISO 3166-1 alpha-2)"
            )
        }

        if (!normalized.all { it.isLetter() }) {
            return ValidationResult(
                valid = false,
                errorMessage = "Country code must contain only letters"
            )
        }

        if (normalized !in VALID_COUNTRY_CODES) {
            return ValidationResult(
                valid = false,
                errorMessage = "Invalid ISO 3166-1 country code: $normalized"
            )
        }

        return ValidationResult(
            valid = true,
            normalizedCode = normalized,
            displayName = COUNTRY_NAMES[normalized]
        )
    }

    /**
     * Validates an ISO 639-1 language code.
     */
    fun validateLanguageCode(code: String?): ValidationResult {
        if (code.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "Language code cannot be null or blank")
        }

        val normalized = code.trim().lowercase()

        if (normalized.length != 2) {
            return ValidationResult(
                valid = false,
                errorMessage = "Language code must be exactly 2 characters (ISO 639-1)"
            )
        }

        if (!normalized.all { it.isLetter() }) {
            return ValidationResult(
                valid = false,
                errorMessage = "Language code must contain only letters"
            )
        }

        if (normalized !in VALID_LANGUAGE_CODES) {
            return ValidationResult(
                valid = false,
                errorMessage = "Invalid ISO 639-1 language code: $normalized"
            )
        }

        return ValidationResult(
            valid = true,
            normalizedCode = normalized,
            displayName = LANGUAGE_NAMES[normalized]
        )
    }

    /**
     * Simple boolean check for country code validity.
     */
    fun isValidCountryCode(code: String?): Boolean = validateCountryCode(code).valid

    /**
     * Simple boolean check for language code validity.
     */
    fun isValidLanguageCode(code: String?): Boolean = validateLanguageCode(code).valid

    /**
     * Normalizes a country code to uppercase.
     */
    fun normalizeCountryCode(code: String): String = code.trim().uppercase()

    /**
     * Normalizes a language code to lowercase.
     */
    fun normalizeLanguageCode(code: String): String = code.trim().lowercase()

    /**
     * Gets the display name for a country code.
     */
    fun getCountryDisplayName(code: String): String? {
        val normalized = normalizeCountryCode(code)
        return if (normalized in VALID_COUNTRY_CODES) {
            COUNTRY_NAMES[normalized]
        } else {
            null
        }
    }

    /**
     * Gets the display name for a language code.
     */
    fun getLanguageDisplayName(code: String): String? {
        val normalized = normalizeLanguageCode(code)
        return if (normalized in VALID_LANGUAGE_CODES) {
            LANGUAGE_NAMES[normalized]
        } else {
            null
        }
    }

    /**
     * Returns all valid country codes.
     */
    fun getAllCountryCodes(): Set<String> = VALID_COUNTRY_CODES

    /**
     * Returns all valid language codes.
     */
    fun getAllLanguageCodes(): Set<String> = VALID_LANGUAGE_CODES

    /**
     * Returns country codes with their display names.
     */
    fun getCountryCodesWithNames(): Map<String, String> = COUNTRY_NAMES

    /**
     * Returns language codes with their display names.
     */
    fun getLanguageCodesWithNames(): Map<String, String> = LANGUAGE_NAMES

    /**
     * Validates a 4-digit year format.
     * Requirements: 13.3
     */
    fun validateYear(year: Int?): ValidationResult {
        if (year == null) {
            return ValidationResult(valid = false, errorMessage = "Year cannot be null")
        }

        // Reasonable year range for books (1450 - Gutenberg press to current year + 1)
        val currentYear = java.time.Year.now().value
        val minYear = 1450
        val maxYear = currentYear + 1

        if (year < minYear || year > maxYear) {
            return ValidationResult(
                valid = false,
                errorMessage = "Year must be between $minYear and $maxYear"
            )
        }

        return ValidationResult(
            valid = true,
            normalizedCode = year.toString()
        )
    }

    /**
     * Validates a year string and parses it.
     */
    fun validateYearString(yearStr: String?): ValidationResult {
        if (yearStr.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "Year cannot be null or blank")
        }

        val trimmed = yearStr.trim()

        if (trimmed.length != 4) {
            return ValidationResult(
                valid = false,
                errorMessage = "Year must be in 4-digit format (YYYY)"
            )
        }

        val year = trimmed.toIntOrNull()
        if (year == null) {
            return ValidationResult(
                valid = false,
                errorMessage = "Year must be a valid number"
            )
        }

        return validateYear(year)
    }

    /**
     * Simple boolean check for year validity.
     */
    fun isValidYear(year: Int?): Boolean = validateYear(year).valid
}
