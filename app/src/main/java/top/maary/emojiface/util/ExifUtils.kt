package top.maary.emojiface.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ExifUtils {
    fun handleImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var rotationDegrees = 0f
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }

        if (rotationDegrees == 0f) {
            return bitmap
        }

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
