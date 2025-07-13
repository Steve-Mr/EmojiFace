package top.maary.emojiface.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class GetBitmapUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(inputUri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        runCatching { // 使用 runCatching 简化 try-catch 和 Result 返回
            context.contentResolver.openInputStream(inputUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
                    ?: throw IllegalArgumentException("Failed to decode bitmap from URI.")
            } ?: throw IllegalStateException("Could not open InputStream from URI.") // 处理 stream 为 null 的情况
        }
    }
}
