package com.bookcover.archive.service.impl

import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Color extraction utility for book covers.
 * Uses k-means clustering to find dominant colors.
 *
 * Requirements: 20.2 - Dominant color extraction
 */
object ColorExtractor {

    /**
     * Extracts the dominant color from an image using k-means clustering.
     *
     * @param image The image to analyze
     * @param numClusters Number of color clusters to find (default 5)
     * @param sampleSize Number of pixels to sample (default 10000)
     * @return Hex color code of the dominant color (e.g., "#FF5733")
     */
    fun extractDominantColor(
        image: BufferedImage,
        numClusters: Int = 5,
        sampleSize: Int = 10000
    ): String {
        val pixels = samplePixels(image, sampleSize)
        if (pixels.isEmpty()) {
            return "#000000"
        }

        val clusters = kMeansClustering(pixels, numClusters)
        val dominantCluster = clusters.maxByOrNull { it.size }
            ?: return "#000000"

        val avgColor = calculateAverageColor(dominantCluster)
        return rgbToHex(avgColor)
    }

    /**
     * Extracts multiple dominant colors from an image.
     *
     * @param image The image to analyze
     * @param numColors Number of colors to extract
     * @param sampleSize Number of pixels to sample
     * @return List of hex color codes sorted by dominance
     */
    fun extractPalette(
        image: BufferedImage,
        numColors: Int = 5,
        sampleSize: Int = 10000
    ): List<String> {
        val pixels = samplePixels(image, sampleSize)
        if (pixels.isEmpty()) {
            return listOf("#000000")
        }

        val clusters = kMeansClustering(pixels, numColors)
        return clusters
            .sortedByDescending { it.size }
            .map { cluster ->
                val avgColor = calculateAverageColor(cluster)
                rgbToHex(avgColor)
            }
    }

    /**
     * Samples pixels from the image, avoiding very light and very dark pixels.
     */
    private fun samplePixels(image: BufferedImage, sampleSize: Int): List<IntArray> {
        val pixels = mutableListOf<IntArray>()
        val width = image.width
        val height = image.height
        val totalPixels = width * height
        val step = maxOf(1, totalPixels / sampleSize)

        var pixelIndex = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixelIndex % step == 0) {
                    val rgb = image.getRGB(x, y)
                    val r = (rgb shr 16) and 0xFF
                    val g = (rgb shr 8) and 0xFF
                    val b = rgb and 0xFF

                    // Skip very light (near white) and very dark (near black) pixels
                    val brightness = (r + g + b) / 3
                    if (brightness in 20..235) {
                        pixels.add(intArrayOf(r, g, b))
                    }
                }
                pixelIndex++
            }
        }

        return pixels
    }

    /**
     * Performs k-means clustering on the pixel colors.
     */
    private fun kMeansClustering(
        pixels: List<IntArray>,
        k: Int,
        maxIterations: Int = 20
    ): List<List<IntArray>> {
        if (pixels.isEmpty() || k <= 0) {
            return emptyList()
        }

        val actualK = minOf(k, pixels.size)

        // Initialize centroids using k-means++ initialization
        val centroids = initializeCentroids(pixels, actualK)

        var clusters: List<MutableList<IntArray>> = List(actualK) { mutableListOf() }

        repeat(maxIterations) {
            // Clear clusters
            clusters = List(actualK) { mutableListOf() }

            // Assign pixels to nearest centroid
            for (pixel in pixels) {
                val nearestIndex = findNearestCentroid(pixel, centroids)
                clusters[nearestIndex].add(pixel)
            }

            // Update centroids
            var converged = true
            for (i in centroids.indices) {
                if (clusters[i].isNotEmpty()) {
                    val newCentroid = calculateAverageColor(clusters[i])
                    if (!centroids[i].contentEquals(newCentroid)) {
                        centroids[i] = newCentroid
                        converged = false
                    }
                }
            }

            if (converged) {
                return@repeat
            }
        }

        return clusters
    }

    /**
     * Initializes centroids using k-means++ algorithm for better initial placement.
     */
    private fun initializeCentroids(pixels: List<IntArray>, k: Int): MutableList<IntArray> {
        val centroids = mutableListOf<IntArray>()

        // First centroid is random
        centroids.add(pixels.random().copyOf())

        // Remaining centroids are chosen with probability proportional to distance squared
        while (centroids.size < k) {
            val distances = pixels.map { pixel ->
                centroids.minOf { centroid -> colorDistance(pixel, centroid) }
            }

            val totalDistance = distances.sum()
            if (totalDistance == 0.0) {
                // All remaining pixels are identical to existing centroids
                centroids.add(pixels.random().copyOf())
                continue
            }

            // Weighted random selection
            var target = Math.random() * totalDistance
            for (i in pixels.indices) {
                target -= distances[i]
                if (target <= 0) {
                    centroids.add(pixels[i].copyOf())
                    break
                }
            }

            // Fallback if we didn't select anything
            if (centroids.size < centroids.size) {
                centroids.add(pixels.random().copyOf())
            }
        }

        return centroids
    }

    /**
     * Finds the index of the nearest centroid to a pixel.
     */
    private fun findNearestCentroid(pixel: IntArray, centroids: List<IntArray>): Int {
        var minDistance = Double.MAX_VALUE
        var nearestIndex = 0

        for (i in centroids.indices) {
            val distance = colorDistance(pixel, centroids[i])
            if (distance < minDistance) {
                minDistance = distance
                nearestIndex = i
            }
        }

        return nearestIndex
    }

    /**
     * Calculates the Euclidean distance between two colors in RGB space.
     */
    private fun colorDistance(c1: IntArray, c2: IntArray): Double {
        val dr = (c1[0] - c2[0]).toDouble()
        val dg = (c1[1] - c2[1]).toDouble()
        val db = (c1[2] - c2[2]).toDouble()
        return sqrt(dr.pow(2) + dg.pow(2) + db.pow(2))
    }

    /**
     * Calculates the average color of a list of pixels.
     */
    private fun calculateAverageColor(pixels: List<IntArray>): IntArray {
        if (pixels.isEmpty()) {
            return intArrayOf(0, 0, 0)
        }

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L

        for (pixel in pixels) {
            sumR += pixel[0]
            sumG += pixel[1]
            sumB += pixel[2]
        }

        val count = pixels.size
        return intArrayOf(
            (sumR / count).toInt(),
            (sumG / count).toInt(),
            (sumB / count).toInt()
        )
    }

    /**
     * Converts RGB values to hex string.
     */
    private fun rgbToHex(rgb: IntArray): String {
        return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2])
    }
}
