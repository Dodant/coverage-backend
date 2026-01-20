package com.bookcover.archive.validation;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for URL formats (website, portfolio URLs).
 *
 * Validates: Requirements 7.2, 7.3
 */
@Component
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?)://[a-zA-Z0-9][-a-zA-Z0-9]*\\.[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$"
    );

    /**
     * Validates a URL string.
     *
     * @param url the URL string to validate
     * @return true if the URL is valid, false otherwise
     */
    public boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(url);

            // Check scheme
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return false;
            }

            // Check host
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            // Validate as URL (checks format)
            uri.toURL();

            return true;
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a website URL.
     * Website URLs must use HTTP or HTTPS protocol.
     *
     * @param url the website URL to validate
     * @return true if the URL is a valid website URL, false otherwise
     */
    public boolean isValidWebsiteUrl(String url) {
        return isValid(url);
    }

    /**
     * Validates a portfolio URL.
     * Portfolio URLs must use HTTP or HTTPS protocol.
     *
     * @param url the portfolio URL to validate
     * @return true if the URL is a valid portfolio URL, false otherwise
     */
    public boolean isValidPortfolioUrl(String url) {
        return isValid(url);
    }

    /**
     * Normalizes a URL by trimming whitespace.
     *
     * @param url the URL to normalize
     * @return the normalized URL, or null if input is null
     */
    public String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.trim();
    }
}
