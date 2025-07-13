package top.maary.emojiface.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.R
import java.io.IOException
import javax.inject.Inject

class SaveImageUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
                // 使用 MediaStore API 保存到公共目录
                val folderName = "FaceMoji"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "facemoji_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH,  "${Environment.DIRECTORY_PICTURES}/$folderName")
                }

                // 插入 MediaStore 并获取 Uri
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("cannot create file")

                // 写入图片数据
                resolver.openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IOException("failed")
                    }
                }?:throw RuntimeException("Failed to save image")

                resolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                    val exifInterface = ExifInterface(pfd.fileDescriptor)
                    exifInterface.setAttribute(ExifInterface.TAG_SOFTWARE, context.getString(R.string.app_name))
                    exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT, context.getString(R.string.created_by))
                    exifInterface.saveAttributes()
                }?: throw IOException("Failed to open ParcelFileDescriptor for EXIF writing.")
        }
    }
}
