package top.maary.emojiface.domain.model

import kotlinx.serialization.Serializable
import java.io.File
import kotlin.math.min

/**
 * Domain model representing a managed avatar asset.
 *
 * @property id Unique identifier for the avatar.
 * @property fileName Physical file name stored on disk.
 * @property relativePath Relative path from context.filesDir (e.g. "avatars/avatar_...").
 * @property displayName User-facing name for this avatar.
 * @property dateAdded Unix timestamp (ms) when imported.
 * @property width Width in pixels of the cropped 1:1 image.
 * @property height Height in pixels of the cropped 1:1 image.
 * @property mimeType MIME type of the stored image (e.g. "image/png").
 * @property sizeBytes Storage size on disk in bytes.
 */
@Serializable
data class Avatar(
    val id: String,
    val fileName: String,
    val relativePath: String,
    val displayName: String,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val sizeBytes: Long
) {
    /**
     * Resolves the actual File on disk given the application's internal filesDir.
     */
    fun resolveFile(filesDir: File): File = File(filesDir, relativePath)

    /**
     * Aspect ratio (width / height). For 1:1 square avatars, this will be 1.0f.
     */
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1.0f

    /**
     * Whether the image format supports alpha transparency channel (e.g. PNG or WebP).
     */
    val isTransparentFormat: Boolean
        get() = mimeType.equals("image/png", ignoreCase = true) ||
                mimeType.equals("image/webp", ignoreCase = true) ||
                fileName.endsWith(".png", ignoreCase = true) ||
                fileName.endsWith(".webp", ignoreCase = true)

    /**
     * Data class holding calculated 1:1 crop coordinates.
     */
    data class CropCoordinates(
        val x: Int,
        val y: Int,
        val size: Int
    )

    companion object {
        /**
         * Calculates 1:1 maximal centered square crop coordinates from raw dimensions.
         *
         * @param rawWidth Original image width.
         * @param rawHeight Original image height.
         * @return [CropCoordinates] specifying origin (x, y) and side length [size].
         */
        fun calculateSquareCrop(rawWidth: Int, rawHeight: Int): CropCoordinates {
            val safeWidth = rawWidth.coerceAtLeast(0)
            val safeHeight = rawHeight.coerceAtLeast(0)
            val s = min(safeWidth, safeHeight)
            val x0 = (safeWidth - s) / 2
            val y0 = (safeHeight - s) / 2
            return CropCoordinates(x = x0, y = y0, size = s)
        }
    }
}
