package com.bookcover.archive.validation

import java.net.URI
import java.net.URISyntaxException

/**
 * URL Validator for website and portfolio URLs.
 *
 * Validates:
 * - Valid URL format
 * - HTTP or HTTPS protocol
 * - Valid domain structure
 *
 * Requirements: 7.2, 7.3
 */
object UrlValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    // Common portfolio/website domains for additional validation hints
    private val KNOWN_PORTFOLIO_DOMAINS = setOf(
        "behance.net",
        "dribbble.com",
        "artstation.com",
        "deviantart.com",
        "instagram.com",
        "linkedin.com",
        "github.com",
        "notion.so",
        "cargo.site",
        "squarespace.com",
        "wix.com",
        "wordpress.com"
    )

    data class ValidationResult(
        val valid: Boolean,
        val normalizedUrl: String? = null,
        val errorMessage: String? = null,
        val scheme: String? = null,
        val host: String? = null,
        val isKnownPortfolioDomain: Boolean = false
    )

    /**
     * Validates a URL string for website/portfolio use.
     */
    fun validate(url: String?): ValidationResult {
        if (url.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "URL cannot be null or blank")
        }

        val trimmedUrl = url.trim()

        // Try to parse the URL
        val uri: URI
        try {
            uri = URI(trimmedUrl)
        } catch (e: URISyntaxException) {
            return ValidationResult(valid = false, errorMessage = "Invalid URL format: ${e.message}")
        }

        // Validate scheme
        val scheme = uri.scheme?.lowercase()
        if (scheme == null) {
            return ValidationResult(valid = false, errorMessage = "URL must include a protocol (http or https)")
        }
        if (scheme !in ALLOWED_SCHEMES) {
            return ValidationResult(valid = false, errorMessage = "URL must use http or https protocol")
        }

        // Validate host
        val host = uri.host
        if (host.isNullOrBlank()) {
            return ValidationResult(valid = false, errorMessage = "URL must include a valid domain")
        }

        // Validate host format (basic domain validation)
        if (!isValidDomain(host)) {
            return ValidationResult(valid = false, errorMessage = "Invalid domain format")
        }

        // Normalize the URL
        val normalizedUrl = normalizeUrl(uri)

        // Check if it's a known portfolio domain
        val isKnownPortfolio = KNOWN_PORTFOLIO_DOMAINS.any { domain ->
            host.equals(domain, ignoreCase = true) || host.endsWith(".$domain", ignoreCase = true)
        }

        return ValidationResult(
            valid = true,
            normalizedUrl = normalizedUrl,
            scheme = scheme,
            host = host,
            isKnownPortfolioDomain = isKnownPortfolio
        )
    }

    /**
     * Simple boolean check for URL validity.
     */
    fun isValid(url: String?): Boolean = validate(url).valid

    /**
     * Validates a website URL (same as validate, alias for clarity).
     */
    fun validateWebsiteUrl(url: String?): ValidationResult = validate(url)

    /**
     * Validates a portfolio URL (same as validate, alias for clarity).
     */
    fun validatePortfolioUrl(url: String?): ValidationResult = validate(url)

    /**
     * Normalizes a URL by ensuring consistent formatting.
     */
    fun normalizeUrl(url: String): String? {
        return try {
            val uri = URI(url.trim())
            normalizeUrl(uri)
        } catch (e: URISyntaxException) {
            null
        }
    }

    private fun normalizeUrl(uri: URI): String {
        val scheme = uri.scheme.lowercase()
        val host = uri.host.lowercase()
        val port = if (uri.port == -1 ||
                       (scheme == "http" && uri.port == 80) ||
                       (scheme == "https" && uri.port == 443)) {
            ""
        } else {
            ":${uri.port}"
        }
        val path = uri.path?.let { if (it == "/") "" else it } ?: ""
        val query = uri.query?.let { "?$it" } ?: ""
        val fragment = uri.fragment?.let { "#$it" } ?: ""

        return "$scheme://$host$port$path$query$fragment"
    }

    /**
     * Validates domain format.
     */
    private fun isValidDomain(domain: String): Boolean {
        // Basic domain validation rules:
        // - Must not be empty
        // - Must not start or end with hyphen or dot
        // - Must contain at least one dot (for TLD)
        // - Each label must be 1-63 characters
        // - Total length must not exceed 253 characters

        if (domain.isEmpty() || domain.length > 253) {
            return false
        }

        if (domain.startsWith(".") || domain.endsWith(".") ||
            domain.startsWith("-") || domain.endsWith("-")) {
            return false
        }

        // Allow localhost for development
        if (domain == "localhost") {
            return true
        }

        // Must have at least one dot (TLD)
        if (!domain.contains(".")) {
            return false
        }

        val labels = domain.split(".")
        for (label in labels) {
            if (label.isEmpty() || label.length > 63) {
                return false
            }
            if (label.startsWith("-") || label.endsWith("-")) {
                return false
            }
            // Each label must contain only alphanumeric characters and hyphens
            if (!label.all { it.isLetterOrDigit() || it == '-' }) {
                return false
            }
        }

        return true
    }

    /**
     * Extracts the domain from a URL.
     */
    fun extractDomain(url: String): String? {
        return try {
            URI(url.trim()).host?.lowercase()
        } catch (e: URISyntaxException) {
            null
        }
    }
}
