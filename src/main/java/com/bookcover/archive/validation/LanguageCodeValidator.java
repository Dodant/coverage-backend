package com.bookcover.archive.validation;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Validator for ISO 639-1 language codes.
 *
 * Validates: Requirements 13.2
 */
@Component
public class LanguageCodeValidator {

    private static final Set<String> VALID_LANGUAGE_CODES;

    static {
        VALID_LANGUAGE_CODES = Set.of(Locale.getISOLanguages());
    }

    /**
     * Validates an ISO 639-1 language code.
     *
     * @param languageCode the language code to validate
     * @return true if the language code is valid, false otherwise
     */
    public boolean isValid(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return false;
        }
        return VALID_LANGUAGE_CODES.contains(languageCode.toLowerCase());
    }

    /**
     * Normalizes a language code to lowercase.
     *
     * @param languageCode the language code to normalize
     * @return the normalized language code, or null if input is null
     */
    public String normalize(String languageCode) {
        if (languageCode == null) {
            return null;
        }
        return languageCode.trim().toLowerCase();
    }

    /**
     * Gets all valid ISO 639-1 language codes.
     *
     * @return set of valid language codes
     */
    public Set<String> getValidLanguageCodes() {
        return VALID_LANGUAGE_CODES;
    }
}
