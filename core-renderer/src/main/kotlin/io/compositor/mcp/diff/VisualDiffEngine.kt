package io.compositor.mcp.diff

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Result data class for visual regression comparison between two render passes.
 */
data class VisualDiffResult(
    val hasVisualDifferences: Boolean,
    val differencePercentage: Double,
    val diffImageBase64: String?,
    val pixelCountDifferent: Long,
    val totalPixels: Long,
    val summary: String
)

/**
 * Configuration options for image comparison.
 */
data class VisualDiffOptions(
    val threshold: Double = 0.1,
    val diffColorRgb: Int = 0xFFFF007F.toInt() // Magenta overlay
)

/**
 * Pure Kotlin in-memory visual regression engine comparing rasterized previews.
 */
object VisualDiffEngine {

    /**
     * Compares two PNG byte arrays and generates diff metrics and a visual overlay image.
     */
    fun compareImages(
        currentBytes: ByteArray,
        baselineBytes: ByteArray,
        options: VisualDiffOptions = VisualDiffOptions()
    ): VisualDiffResult {
        val currentImg = ImageIO.read(ByteArrayInputStream(currentBytes))
            ?: error("Failed to decode current image PNG bytes")
        val baselineImg = ImageIO.read(ByteArrayInputStream(baselineBytes))
            ?: error("Failed to decode baseline image PNG bytes")

        return compareBufferedImages(currentImg, baselineImg, options)
    }

    /**
     * Compares two image files on disk.
     */
    fun compareFiles(
        currentFile: File,
        baselineFile: File,
        options: VisualDiffOptions = VisualDiffOptions()
    ): VisualDiffResult {
        require(currentFile.exists()) { "Current image file not found: ${currentFile.path}" }
        require(baselineFile.exists()) { "Baseline image file not found: ${baselineFile.path}" }

        return compareImages(currentFile.readBytes(), baselineFile.readBytes(), options)
    }

    /**
     * Compares two [BufferedImage] instances pixel by pixel.
     */
    fun compareBufferedImages(
        img1: BufferedImage,
        img2: BufferedImage,
        options: VisualDiffOptions = VisualDiffOptions()
    ): VisualDiffResult {
        val width = max(img1.width, img2.width)
        val height = max(img1.height, img2.height)

        val normalized1 = normalizeImage(img1, width, height)
        val normalized2 = normalizeImage(img2, width, height)

        val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var differentPixels = 0L
        val maxColorDistance = 255.0 * 3.0 * options.threshold

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb1 = normalized1.getRGB(x, y)
                val rgb2 = normalized2.getRGB(x, y)

                if (isPixelDifferent(rgb1, rgb2, maxColorDistance)) {
                    differentPixels++
                    diffImage.setRGB(x, y, options.diffColorRgb)
                } else {
                    // Dim unchanged pixels to provide spatial context behind the diff overlay
                    val dimmed = dimPixel(rgb1)
                    diffImage.setRGB(x, y, dimmed)
                }
            }
        }

        val totalPixels = width.toLong() * height.toLong()
        val diffPercentage = if (totalPixels > 0) {
            val rawPercent = (differentPixels.toDouble() / totalPixels.toDouble()) * 100.0
            (rawPercent * 100.0).roundToInt() / 100.0
        } else {
            0.0
        }

        val hasDifferences = differentPixels > 0L
        val diffBase64 = if (hasDifferences) encodePngBase64(diffImage) else null

        val summary = if (hasDifferences) {
            "Visual regression detected: $diffPercentage% difference " +
                "($differentPixels of $totalPixels pixels modified)."
        } else {
            "No visual regression detected (0.0% difference, $totalPixels pixels match baseline)."
        }

        return VisualDiffResult(
            hasVisualDifferences = hasDifferences,
            differencePercentage = diffPercentage,
            diffImageBase64 = diffBase64,
            pixelCountDifferent = differentPixels,
            totalPixels = totalPixels,
            summary = summary
        )
    }

    private fun normalizeImage(src: BufferedImage, targetW: Int, targetH: Int): BufferedImage {
        if (src.width == targetW && src.height == targetH && src.type == BufferedImage.TYPE_INT_ARGB) {
            return src
        }
        val padded = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
        val g = padded.createGraphics()
        try {
            g.drawImage(src, 0, 0, null)
        } finally {
            g.dispose()
        }
        return padded
    }

    private fun isPixelDifferent(rgb1: Int, rgb2: Int, maxDistance: Double): Boolean {
        if (rgb1 == rgb2) return false

        val a1 = (rgb1 ushr 24) and 0xFF
        val a2 = (rgb2 ushr 24) and 0xFF
        if (abs(a1 - a2) > 10) return true

        val r1 = (rgb1 ushr 16) and 0xFF
        val r2 = (rgb2 ushr 16) and 0xFF
        val g1 = (rgb1 ushr 8) and 0xFF
        val g2 = (rgb2 ushr 8) and 0xFF
        val b1 = rgb1 and 0xFF
        val b2 = rgb2 and 0xFF

        val delta = abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
        return delta > maxDistance
    }

    private fun dimPixel(rgb: Int): Int {
        val a = (rgb ushr 24) and 0xFF
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        val gray = ((0.299 * r + 0.587 * g + 0.114 * b) * 0.4).roundToInt().coerceIn(0, 255)
        val alpha = (a * 0.5).roundToInt().coerceIn(0, 255)
        return (alpha shl 24) or (gray shl 16) or (gray shl 8) or gray
    }

    private fun encodePngBase64(image: BufferedImage): String {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }
}
