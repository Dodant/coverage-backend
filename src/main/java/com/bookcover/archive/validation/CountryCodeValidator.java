package com.bookcover.archive.validation;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for ISO 3166-1 alpha-2 country codes.
 *
 * Validates: Requirements 13.1
 */
@Component
public class CountryCodeValidator {

    private static final Set<String> VALID_COUNTRY_CODES;

    static {
        VALID_COUNTRY_CODES = Set.of(Locale.getISOCountries());
    }

    /**
     * Validates an ISO 3166-1 alpha-2 country code.
     *
     * @param countryCode the country code to validate
     * @return true if the country code is valid, false otherwise
     */
    public boolean isValid(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return false;
        }
        return VALID_COUNTRY_CODES.contains(countryCode.toUpperCase());
    }

    /**
     * Normalizes a country code to uppercase.
     *
     * @param countryCode the country code to normalize
     * @return the normalized country code, or null if input is null
     */
    public String normalize(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        return countryCode.trim().toUpperCase();
    }

    /**
     * Gets all valid ISO 3166-1 alpha-2 country codes.
     *
     * @return set of valid country codes
     */
    public Set<String> getValidCountryCodes() {
        return VALID_COUNTRY_CODES;
    }
}
