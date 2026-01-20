package com.bookcover.archive.validation

import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * Image Validator for book cover uploads.
 *
 * Validates:
 * - Minimum resolution: 800x1200 pixels
 * - Supported formats: JPEG, PNG, WebP
 * - Maximum file size: 10MB
 *
 * Requirements: 14.1, 14.4, 14.5
 */
object ImageValidator {

    const val MIN_WIDTH = 800
    const val MIN_HEIGHT = 1200
    const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10MB

    val SUPPORTED_CONTENT_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    val SUPPORTED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp"
    )

    // Magic bytes for file type detection
    private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val WEBP_MAGIC = "RIFF".toByteArray() // WEBP starts with RIFF....WEBP

    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String> = emptyList(),
        val width: Int? = null,
        val height: Int? = null,
        val fileSize: Long? = null,
        val detectedFormat: String? = null
    )

    data class ImageMetadata(
        val width: Int,
        val height: Int,
        val format: String
    )

    /**
     * Validates a MultipartFile for book cover requirements.
     */
    fun validate(file: MultipartFile): ValidationResult {
        val errors = mutableListOf<String>()
        var width: Int? = null
        var height: Int? = null
        var detectedFormat: String? = null

        // Check if file is empty
        if (file.isEmpty) {
            return ValidationResult(valid = false, errors = listOf("File is empty"))
        }

        // Validate file size
        val fileSize = file.size
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            errors.add("File size (${formatFileSize(fileSize)}) exceeds maximum allowed size (${formatFileSize(MAX_FILE_SIZE_BYTES)})")
        }

        // Validate content type
        val contentType = file.contentType
        if (contentType == null || contentType !in SUPPORTED_CONTENT_TYPES) {
            errors.add("Unsupported file format. Supported formats: JPEG, PNG, WebP")
        }

        // Validate file extension
        val extension = file.originalFilename?.substringAfterLast(".")?.lowercase()
        if (extension == null || extension !in SUPPORTED_EXTENSIONS) {
            errors.add("Invalid file extension. Supported extensions: ${SUPPORTED_EXTENSIONS.joinToString(", ")}")
        }

        // Validate actual file content (magic bytes)
        try {
            val actualFormat = detectImageFormat(file.inputStream)
            if (actualFormat == null) {
                errors.add("File content does not match a supported image format")
            } else {
                detectedFormat = actualFormat
            }
        } catch (e: Exception) {
            errors.add("Unable to read file content: ${e.message}")
        }

        // Validate image dimensions
        try {
            val metadata = readImageMetadata(file.inputStream)
            if (metadata != null) {
                width = metadata.width
                height = metadata.height

                if (metadata.width < MIN_WIDTH || metadata.height < MIN_HEIGHT) {
                    errors.add("Image resolution (${metadata.width}x${metadata.height}) is below minimum required (${MIN_WIDTH}x${MIN_HEIGHT})")
                }
            } else {
                errors.add("Unable to read image dimensions")
            }
        } catch (e: Exception) {
            errors.add("Unable to process image: ${e.message}")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            width = width,
            height = height,
            fileSize = fileSize,
            detectedFormat = detectedFormat
        )
    }

    /**
     * Validates image dimensions only.
     */
    fun validateDimensions(width: Int, height: Int): ValidationResult {
        val errors = mutableListOf<String>()

        if (width < MIN_WIDTH) {
            errors.add("Image width ($width) is below minimum required ($MIN_WIDTH)")
        }
        if (height < MIN_HEIGHT) {
            errors.add("Image height ($height) is below minimum required ($MIN_HEIGHT)")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            width = width,
            height = height
        )
    }

    /**
     * Validates file size only.
     */
    fun validateFileSize(sizeInBytes: Long): ValidationResult {
        val errors = mutableListOf<String>()

        if (sizeInBytes <= 0) {
            errors.add("File size must be greater than 0")
        }
        if (sizeInBytes > MAX_FILE_SIZE_BYTES) {
            errors.add("File size (${formatFileSize(sizeInBytes)}) exceeds maximum allowed size (${formatFileSize(MAX_FILE_SIZE_BYTES)})")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            fileSize = sizeInBytes
        )
    }

    /**
     * Validates file format by content type.
     */
    fun validateFormat(contentType: String?): ValidationResult {
        val errors = mutableListOf<String>()

        if (contentType == null) {
            errors.add("Content type is required")
        } else if (contentType !in SUPPORTED_CONTENT_TYPES) {
            errors.add("Unsupported content type: $contentType. Supported: ${SUPPORTED_CONTENT_TYPES.joinToString(", ")}")
        }

        return ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            detectedFormat = if (contentType in SUPPORTED_CONTENT_TYPES) contentType else null
        )
    }

    /**
     * Detects image format from magic bytes.
     */
    fun detectImageFormat(inputStream: InputStream): String? {
        val header = ByteArray(12)
        val bytesRead = inputStream.read(header)

        if (bytesRead < 3) return null

        return when {
            header.take(3).toByteArray().contentEquals(JPEG_MAGIC) -> "image/jpeg"
            header.take(8).toByteArray().contentEquals(PNG_MAGIC) -> "image/png"
            isWebP(header) -> "image/webp"
            else -> null
        }
    }

    /**
     * Reads image metadata (dimensions and format).
     */
    fun readImageMetadata(inputStream: InputStream): ImageMetadata? {
        return try {
            val imageInputStream = ImageIO.createImageInputStream(inputStream)
            val readers = ImageIO.getImageReaders(imageInputStream)

            if (readers.hasNext()) {
                val reader = readers.next()
                reader.input = imageInputStream

                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                val format = reader.formatName

                reader.dispose()
                imageInputStream.close()

                ImageMetadata(width, height, format)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isWebP(header: ByteArray): Boolean {
        // WebP format: RIFF....WEBP
        if (header.size < 12) return false
        val riff = String(header.sliceArray(0..3))
        val webp = String(header.sliceArray(8..11))
        return riff == "RIFF" && webp == "WEBP"
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
}
