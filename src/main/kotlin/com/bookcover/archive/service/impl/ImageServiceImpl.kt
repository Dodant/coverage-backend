package com.bookcover.archive.service.impl

import com.bookcover.archive.config.ImageProperties
import com.bookcover.archive.config.StorageProperties
import com.bookcover.archive.exception.ImageProcessingException
import com.bookcover.archive.exception.ImageValidationException
import com.bookcover.archive.exception.StorageException
import com.bookcover.archive.service.ImageService
import com.bookcover.archive.service.ImageType
import com.bookcover.archive.service.ImageUploadResult
import com.bookcover.archive.service.ImageValidationResult
import com.bookcover.archive.validation.ImageValidator
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.imageio.ImageIO

/**
 * Implementation of ImageService for image processing and storage.
 *
 * Requirements:
 * - 14.7: sRGB color space conversion
 * - 14.8: Thumbnail generation and storage optimization
 * - 20.2: Dominant color extraction
 */
@Service
class ImageServiceImpl(
    private val storageProperties: StorageProperties,
    private val imageProperties: ImageProperties,
    private val s3Client: S3Client?
) : ImageService {

    private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

    override fun uploadImage(file: MultipartFile, type: ImageType): ImageUploadResult {
        // Validate the image first
        val validationResult = validateImage(file)
        if (!validationResult.valid) {
            throw ImageValidationException(
                "Image validation failed",
                validationResult.errors
            )
        }

        try {
            // Read the image
            val originalImage = ImageIO.read(file.inputStream)
                ?: throw ImageProcessingException("Unable to read image file")

            // Convert to sRGB color space
            val srgbImage = convertImageToSRGB(originalImage)

            // Determine output format
            val format = getOutputFormat(file.contentType ?: "image/jpeg")

            // Convert to bytes
            val originalBytes = imageToBytes(srgbImage, format)

            // Generate thumbnail
            val thumbnailImage = generateThumbnailImage(srgbImage)
            val thumbnailBytes = imageToBytes(thumbnailImage, format)

            // Extract dominant color
            val dominantColor = extractDominantColorFromImage(srgbImage)

            // Generate unique file names
            val fileId = UUID.randomUUID().toString()
            val extension = formatToExtension(format)
            val originalKey = "${type.name.lowercase()}/$fileId/original.$extension"
            val thumbnailKey = "${type.name.lowercase()}/$fileId/thumbnail.$extension"

            // Upload to storage
            val originalUrl = uploadToStorage(originalKey, originalBytes, "image/$format")
            val thumbnailUrl = uploadToStorage(thumbnailKey, thumbnailBytes, "image/$format")

            return ImageUploadResult(
                originalUrl = originalUrl,
                thumbnailUrl = thumbnailUrl,
                width = srgbImage.width,
                height = srgbImage.height,
                fileSize = originalBytes.size.toLong(),
                format = format,
                dominantColor = dominantColor
            )
        } catch (e: ImageValidationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to upload image", e)
            throw ImageProcessingException("Failed to process and upload image: ${e.message}", e)
        }
    }

    override fun validateImage(file: MultipartFile): ImageValidationResult {
        val result = ImageValidator.validate(file)
        return ImageValidationResult(
            valid = result.valid,
            errors = result.errors,
            width = result.width,
            height = result.height,
            fileSize = result.fileSize,
            detectedFormat = result.detectedFormat
        )
    }

    override fun generateThumbnail(imageUrl: String): String {
        try {
            val imageBytes = downloadImage(imageUrl)
            val image = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: throw ImageProcessingException("Unable to read image from URL: $imageUrl")

            val thumbnail = generateThumbnailImage(image)
            val format = detectFormatFromUrl(imageUrl)
            val thumbnailBytes = imageToBytes(thumbnail, format)

            // Generate thumbnail key from original URL
            val thumbnailKey = imageUrl.replace("/original.", "/thumbnail.")
                .substringAfter(storageProperties.bucket + "/")

            return uploadToStorage(thumbnailKey, thumbnailBytes, "image/$format")
        } catch (e: Exception) {
            logger.error("Failed to generate thumbnail for: $imageUrl", e)
            throw ImageProcessingException("Failed to generate thumbnail: ${e.message}", e)
        }
    }

    override fun extractDominantColor(imageUrl: String): String {
        try {
            val imageBytes = downloadImage(imageUrl)
            return extractDominantColor(imageBytes)
        } catch (e: Exception) {
            logger.error("Failed to extract dominant color from: $imageUrl", e)
            throw ImageProcessingException("Failed to extract dominant color: ${e.message}", e)
        }
    }

    override fun extractDominantColor(imageBytes: ByteArray): String {
        try {
            val image = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: throw ImageProcessingException("Unable to read image for color extraction")

            return extractDominantColorFromImage(image)
        } catch (e: Exception) {
            logger.error("Failed to extract dominant color", e)
            throw ImageProcessingException("Failed to extract dominant color: ${e.message}", e)
        }
    }

    override fun convertToSRGB(imageBytes: ByteArray, format: String): ByteArray {
        try {
            return ColorSpaceConverter.convertToSRGB(imageBytes, format)
        } catch (e: Exception) {
            logger.error("Failed to convert image to sRGB", e)
            throw ImageProcessingException("Failed to convert to sRGB: ${e.message}", e)
        }
    }

    override fun deleteImage(imageUrl: String) {
        try {
            when (storageProperties.type) {
                "s3", "minio" -> deleteFromS3(imageUrl)
                "local" -> deleteFromLocal(imageUrl)
                else -> logger.warn("Unknown storage type: ${storageProperties.type}")
            }
        } catch (e: Exception) {
            logger.error("Failed to delete image: $imageUrl", e)
            // Don't throw - deletion failures shouldn't break the flow
        }
    }


    // ==================== Private Helper Methods ====================

    /**
     * Converts a BufferedImage to sRGB color space.
     * Requirements: 14.7
     */
    private fun convertImageToSRGB(image: BufferedImage): BufferedImage {
        return ColorSpaceConverter.convertToSRGB(image)
    }

    /**
     * Generates a thumbnail image.
     * Requirements: 14.8
     */
    private fun generateThumbnailImage(image: BufferedImage): BufferedImage {
        val targetWidth = imageProperties.thumbnail.width
        val targetHeight = imageProperties.thumbnail.height

        // Use Scalr for high-quality thumbnail generation
        return Scalr.resize(
            image,
            Scalr.Method.QUALITY,
            Scalr.Mode.FIT_TO_WIDTH,
            targetWidth,
            targetHeight,
            Scalr.OP_ANTIALIAS
        )
    }

    /**
     * Extracts the dominant color from an image using k-means clustering.
     * Requirements: 20.2
     */
    private fun extractDominantColorFromImage(image: BufferedImage): String {
        return ColorExtractor.extractDominantColor(image)
    }

    /**
     * Converts a BufferedImage to byte array.
     */
    private fun imageToBytes(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Gets the output format from content type.
     */
    private fun getOutputFormat(contentType: String): String {
        return when (contentType) {
            "image/jpeg" -> "jpeg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpeg"
        }
    }

    /**
     * Gets file extension from format.
     */
    private fun formatToExtension(format: String): String {
        return when (format) {
            "jpeg" -> "jpg"
            else -> format
        }
    }

    /**
     * Detects format from URL.
     */
    private fun detectFormatFromUrl(url: String): String {
        val extension = url.substringAfterLast(".").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "jpeg"
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpeg"
        }
    }

    // ==================== Storage Operations ====================

    /**
     * Uploads bytes to storage and returns the URL.
     */
    private fun uploadToStorage(key: String, bytes: ByteArray, contentType: String): String {
        return when (storageProperties.type) {
            "s3", "minio" -> uploadToS3(key, bytes, contentType)
            "local" -> uploadToLocal(key, bytes)
            else -> throw StorageException("Unknown storage type: ${storageProperties.type}")
        }
    }

    /**
     * Uploads to S3/MinIO.
     */
    private fun uploadToS3(key: String, bytes: ByteArray, contentType: String): String {
        val client = s3Client ?: throw StorageException("S3 client not configured")

        val request = PutObjectRequest.builder()
            .bucket(storageProperties.bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .build()

        client.putObject(request, RequestBody.fromBytes(bytes))

        // Return the URL
        return if (!storageProperties.endpoint.isNullOrBlank()) {
            "${storageProperties.endpoint}/${storageProperties.bucket}/$key"
        } else {
            "https://${storageProperties.bucket}.s3.${storageProperties.region}.amazonaws.com/$key"
        }
    }

    /**
     * Uploads to local file system.
     */
    private fun uploadToLocal(key: String, bytes: ByteArray): String {
        val basePath = Paths.get(storageProperties.localPath)
        val filePath = basePath.resolve(key)

        // Create parent directories
        Files.createDirectories(filePath.parent)

        // Write file
        Files.write(filePath, bytes)

        return "file://${filePath.toAbsolutePath()}"
    }

    /**
     * Downloads image from URL.
     */
    private fun downloadImage(imageUrl: String): ByteArray {
        return when {
            imageUrl.startsWith("file://") -> {
                val path = Paths.get(imageUrl.removePrefix("file://"))
                Files.readAllBytes(path)
            }
            imageUrl.contains(storageProperties.bucket) && s3Client != null -> {
                downloadFromS3(imageUrl)
            }
            else -> {
                URL(imageUrl).readBytes()
            }
        }
    }

    /**
     * Downloads from S3/MinIO.
     */
    private fun downloadFromS3(imageUrl: String): ByteArray {
        val client = s3Client ?: throw StorageException("S3 client not configured")

        val key = imageUrl.substringAfter("${storageProperties.bucket}/")

        val request = GetObjectRequest.builder()
            .bucket(storageProperties.bucket)
            .key(key)
            .build()

        return client.getObject(request).readAllBytes()
    }

    /**
     * Deletes from S3/MinIO.
     */
    private fun deleteFromS3(imageUrl: String) {
        val client = s3Client ?: return

        val key = imageUrl.substringAfter("${storageProperties.bucket}/")

        val request = DeleteObjectRequest.builder()
            .bucket(storageProperties.bucket)
            .key(key)
            .build()

        client.deleteObject(request)
    }

    /**
     * Deletes from local file system.
     */
    private fun deleteFromLocal(imageUrl: String) {
        if (imageUrl.startsWith("file://")) {
            val path = Paths.get(imageUrl.removePrefix("file://"))
            Files.deleteIfExists(path)
        }
    }
}
