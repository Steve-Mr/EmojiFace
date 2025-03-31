package top.maary.emojiface.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class GenerateShareableUriUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cachePath, "shared_${System.currentTimeMillis()}.png").apply {
                FileOutputStream(this).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
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
