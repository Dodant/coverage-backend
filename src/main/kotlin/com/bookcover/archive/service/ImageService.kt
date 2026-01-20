package com.bookcover.archive.service

import org.springframework.web.multipart.MultipartFile

/**
 * Service interface for image processing operations.
 *
 * Requirements:
 * - 14.7: sRGB color space conversion
 * - 14.8: Thumbnail generation and storage optimization
 * - 20.2: Dominant color extraction
 */
interface ImageService {

    /**
     * Uploads an image to storage and generates a thumbnail.
     * Requirements: 14.7, 14.8
     *
     * @param file The image file to upload
     * @param type The type of image (COVER, REDESIGN, PROFILE)
     * @return Upload result containing URLs and metadata
     * @throws ImageValidationException if image validation fails
     * @throws ImageProcessingException if processing fails
     */
    fun uploadImage(file: MultipartFile, type: ImageType): ImageUploadResult

    /**
     * Validates an image file without uploading.
     *
     * @param file The image file to validate
     * @return Validation result
     */
    fun validateImage(file: MultipartFile): ImageValidationResult

    /**
     * Generates a thumbnail from an existing image URL.
     * Requirements: 14.8
     *
     * @param imageUrl The URL of the original image
     * @return URL of the generated thumbnail
     * @throws ImageProcessingException if thumbnail generation fails
     */
    fun generateThumbnail(imageUrl: String): String

    /**
     * Extracts the dominant color from an image.
     * Requirements: 20.2
     *
     * @param imageUrl The URL of the image
     * @return Hex color code (e.g., "#FF5733")
     * @throws ImageProcessingException if color extraction fails
     */
    fun extractDominantColor(imageUrl: String): String

    /**
     * Extracts the dominant color from image bytes.
     * Requirements: 20.2
     *
     * @param imageBytes The image data
     * @return Hex color code (e.g., "#FF5733")
     * @throws ImageProcessingException if color extraction fails
     */
    fun extractDominantColor(imageBytes: ByteArray): String

    /**
     * Converts an image to sRGB color space.
     * Requirements: 14.7
     *
     * @param imageBytes The image data
     * @param format The output format (jpeg, png, webp)
     * @return Converted image bytes in sRGB color space
     * @throws ImageProcessingException if conversion fails
     */
    fun convertToSRGB(imageBytes: ByteArray, format: String): ByteArray

    /**
     * Deletes an image from storage.
     *
     * @param imageUrl The URL of the image to delete
     */
    fun deleteImage(imageUrl: String)
}

/**
 * Type of image being processed.
 */
enum class ImageType {
    /** Book cover image */
    COVER,
    /** Redesign image */
    REDESIGN,
    /** User profile image */
    PROFILE
}

/**
 * Result of an image upload operation.
 */
data class ImageUploadResult(
    /** URL to the original full-resolution image */
    val originalUrl: String,
    /** URL to the thumbnail image */
    val thumbnailUrl: String,
    /** Image width in pixels */
    val width: Int,
    /** Image height in pixels */
    val height: Int,
    /** File size in bytes */
    val fileSize: Long,
    /** Detected image format */
    val format: String,
    /** Dominant color in hex format */
    val dominantColor: String? = null
)

/**
 * Result of image validation.
 */
data class ImageValidationResult(
    /** Whether the image is valid */
    val valid: Boolean,
    /** List of validation errors */
    val errors: List<String> = emptyList(),
    /** Image width if readable */
    val width: Int? = null,
    /** Image height if readable */
    val height: Int? = null,
    /** File size in bytes */
    val fileSize: Long? = null,
    /** Detected format */
    val detectedFormat: String? = null
)
