package com.bookcover.archive.service.impl

import java.awt.RenderingHints
import java.awt.color.ColorSpace
import java.awt.color.ICC_ColorSpace
import java.awt.color.ICC_Profile
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Color space conversion utility for standardizing images to sRGB.
 *
 * Requirements: 14.7 - sRGB color space conversion
 *
 * This ensures all uploaded images are stored in a consistent color space
 * for accurate display across different devices and browsers.
 */
object ColorSpaceConverter {

    // Standard sRGB color space
    private val SRGB_COLOR_SPACE: ColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB)

    /**
     * Converts an image to sRGB color space.
     *
     * @param image The source image
     * @return A new BufferedImage in sRGB color space
     */
    fun convertToSRGB(image: BufferedImage): BufferedImage {
        val sourceColorSpace = image.colorModel.colorSpace

        // If already sRGB, return a copy to ensure consistent type
        if (sourceColorSpace.isCS_sRGB) {
            return ensureRGBType(image)
        }

        // Create color conversion operation with high quality rendering
        val hints = RenderingHints(
            RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY
        )
        hints[RenderingHints.KEY_DITHERING] = RenderingHints.VALUE_DITHER_ENABLE

        val colorConvertOp = ColorConvertOp(SRGB_COLOR_SPACE, hints)

        // Create destination image in sRGB
        val srgbImage = BufferedImage(
            image.width,
            image.height,
            if (hasAlpha(image)) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        )

        // Perform conversion
        colorConvertOp.filter(image, srgbImage)

        return srgbImage
    }

    /**
     * Converts image bytes to sRGB color space.
     *
     * @param imageBytes The source image bytes
     * @param outputFormat The output format (jpeg, png, webp)
     * @return Converted image bytes in sRGB color space
     */
    fun convertToSRGB(imageBytes: ByteArray, outputFormat: String): ByteArray {
        val inputStream = ByteArrayInputStream(imageBytes)
        val image = ImageIO.read(inputStream)
            ?: throw IllegalArgumentException("Unable to read image from bytes")

        val srgbImage = convertToSRGB(image)

        val outputStream = ByteArrayOutputStream()
        val formatName = normalizeFormat(outputFormat)

        // For JPEG, ensure no alpha channel
        val outputImage = if (formatName == "jpeg" && hasAlpha(srgbImage)) {
            removeAlphaChannel(srgbImage)
        } else {
            srgbImage
        }

        ImageIO.write(outputImage, formatName, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Checks if an image is in sRGB color space.
     *
     * @param image The image to check
     * @return true if the image is in sRGB color space
     */
    fun isSRGB(image: BufferedImage): Boolean {
        return image.colorModel.colorSpace.isCS_sRGB
    }

    /**
     * Gets the color space name of an image.
     *
     * @param image The image to check
     * @return The color space name
     */
    fun getColorSpaceName(image: BufferedImage): String {
        val colorSpace = image.colorModel.colorSpace

        return when {
            colorSpace.isCS_sRGB -> "sRGB"
            colorSpace is ICC_ColorSpace -> {
                try {
                    val profile = colorSpace.profile
                    getProfileName(profile) ?: "ICC Profile"
                } catch (e: Exception) {
                    "Unknown ICC Profile"
                }
            }
            colorSpace.type == ColorSpace.TYPE_CMYK -> "CMYK"
            colorSpace.type == ColorSpace.TYPE_GRAY -> "Grayscale"
            colorSpace.type == ColorSpace.TYPE_RGB -> "RGB (non-sRGB)"
            else -> "Unknown (${colorSpace.type})"
        }
    }

    /**
     * Ensures the image is in a standard RGB type for consistent processing.
     */
    private fun ensureRGBType(image: BufferedImage): BufferedImage {
        val targetType = if (hasAlpha(image)) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }

        if (image.type == targetType) {
            return image
        }

        val newImage = BufferedImage(image.width, image.height, targetType)
        val graphics = newImage.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return newImage
    }

    /**
     * Checks if an image has an alpha channel.
     */
    private fun hasAlpha(image: BufferedImage): Boolean {
        return image.colorModel.hasAlpha()
    }

    /**
     * Removes the alpha channel from an image (for JPEG output).
     */
    private fun removeAlphaChannel(image: BufferedImage): BufferedImage {
        val newImage = BufferedImage(
            image.width,
            image.height,
            BufferedImage.TYPE_INT_RGB
        )

        val graphics = newImage.createGraphics()
        // Fill with white background for transparency
        graphics.color = java.awt.Color.WHITE
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()

        return newImage
    }

    /**
     * Gets the profile name from an ICC profile.
     */
    private fun getProfileName(profile: ICC_Profile): String? {
        return try {
            val descTag = profile.getData(ICC_Profile.icSigProfileDescriptionTag)
            if (descTag != null && descTag.size > 12) {
                // Parse the description tag (simplified)
                String(descTag, 12, minOf(descTag.size - 12, 100))
                    .trim()
                    .takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Normalizes format string for ImageIO.
     */
    private fun normalizeFormat(format: String): String {
        return when (format.lowercase()) {
            "jpg", "jpeg" -> "jpeg"
            "png" -> "png"
            "webp" -> "webp"
            "gif" -> "gif"
            else -> "jpeg"
        }
    }
}
