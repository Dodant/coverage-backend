package com.bookcover.archive.domain.tag

/**
 * Categories for design tags used to classify book cover design attributes.
 * Used for filtering and browsing covers by design characteristics.
 *
 * Requirements:
 * - 20.1: Support design attribute tags (typography, illustration, photo, minimal, retro, etc.)
 */
enum class TagCategory {
    /** Typography-related tags (e.g., serif, sans-serif, handwritten) */
    STYLE,

    /** Color-related tags (e.g., monochrome, vibrant, pastel) */
    COLOR,

    /** Layout-related tags (e.g., centered, asymmetric, full-bleed) */
    LAYOUT,

    /** Typography style tags (e.g., bold, minimal text, decorative) */
    TYPOGRAPHY,

    /** Illustration style tags (e.g., hand-drawn, digital, photographic) */
    ILLUSTRATION
}
