package com.bookcover.archive.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

/**
 * Configuration properties for image storage.
 */
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    /** Storage type: s3, minio, or local */
    val type: String = "local",
    /** S3/MinIO endpoint URL */
    val endpoint: String? = null,
    /** Access key for S3/MinIO */
    val accessKey: String? = null,
    /** Secret key for S3/MinIO */
    val secretKey: String? = null,
    /** S3 bucket name */
    val bucket: String = "book-covers",
    /** AWS region */
    val region: String = "us-east-1",
    /** Local storage path (for local type) */
    val localPath: String = "/tmp/book-covers"
)

/**
 * Configuration properties for image processing.
 */
@ConfigurationProperties(prefix = "image")
data class ImageProperties(
    /** Minimum image width in pixels */
    val minWidth: Int = 800,
    /** Minimum image height in pixels */
    val minHeight: Int = 1200,
    /** Maximum file size in bytes */
    val maxFileSize: Long = 10 * 1024 * 1024,
    /** Allowed image formats */
    val allowedFormats: List<String> = listOf("image/jpeg", "image/png", "image/webp"),
    /** Thumbnail configuration */
    val thumbnail: ThumbnailProperties = ThumbnailProperties()
)

/**
 * Configuration properties for thumbnail generation.
 */
data class ThumbnailProperties(
    /** Thumbnail width in pixels */
    val width: Int = 300,
    /** Thumbnail height in pixels */
    val height: Int = 450
)

/**
 * Storage configuration for S3/MinIO client.
 */
@Configuration
class StorageConfig(
    private val storageProperties: StorageProperties
) {

    /**
     * Creates an S3 client for S3 or MinIO storage.
     * Only created when storage type is s3 or minio.
     */
    @Bean
    fun s3Client(): S3Client? {
        if (storageProperties.type == "local") {
            return null
        }

        val credentials = AwsBasicCredentials.create(
            storageProperties.accessKey ?: "",
            storageProperties.secretKey ?: ""
        )

        val builder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(storageProperties.region))

        // For MinIO or custom S3-compatible storage
        if (!storageProperties.endpoint.isNullOrBlank()) {
            builder.endpointOverride(URI.create(storageProperties.endpoint))
            builder.forcePathStyle(true)
        }

        return builder.build()
    }
}
