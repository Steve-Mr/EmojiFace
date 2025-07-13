package top.maary.emojiface.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class GenerateShareableUriUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "shared_${System.currentTimeMillis()}.png").apply {
                FileOutputStream(this).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }

            // --- START EXIF Modification ---
            try {
                val exifInterface = ExifInterface(file.absoluteFile) // <-- Use file path

                exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE, context.getString(R.string.app_name))
                exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, context.getString(R.string.created_by))

                exifInterface.saveAttributes()

                Log.v("MOJIFACE", "SHARE EXIF SAVED")

            } catch (e: IOException) {
                println("Warning: Failed to write EXIF data to ${file.name}: ${e.message}")
            }

            // 生成安全 Uri
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }
}
