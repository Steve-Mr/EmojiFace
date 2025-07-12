package top.maary.emojiface.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.generateShortUniqueId
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ManageFontUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferenceRepository: PreferenceRepository
) {
    suspend fun addFont(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver

            // 从 URI 中获取文件名

            val originalFileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } else null
            }?: ""


            // 提取扩展名（包括点）
            var extension = originalFileName.substringAfterLast('.', "")
            val fileNameWithoutExtension = originalFileName.substringBeforeLast('.')
            if (extension.isNotEmpty()) {
                extension = ".$extension"
            }

            // 定义支持的字体扩展名
            val supportedExtensions = listOf(".ttf", ".otf")
            // 如果提取不到有效扩展名，则根据 MIME 类型推断；否则，若不支持则可选择默认扩展名或拒绝处理
            if (extension.isEmpty() || extension !in supportedExtensions) {
                extension = ".ttf"
            }

            val fileName = "${fileNameWithoutExtension}_${generateShortUniqueId()}$extension"
            val destFile = File(context.filesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            preferenceRepository.addFont(destFile.absolutePath)
        }
    }

    suspend fun removeFont(fontPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (fontPath == Constants.DEFAULT_FONT_MARKER) {
                throw IllegalArgumentException("Cannot remove the default font marker.")
            }
            val file = File(fontPath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    throw RuntimeException("Failed to delete font file: $fontPath")
                }
            }
            // 从 Preferences 中移除
            preferenceRepository.removeFont(fontPath)

            // 如果移除的是当前选中的字体，需要重置为默认字体
            val currentSelected = preferenceRepository.selectedFont.first()
            if (currentSelected == fontPath) {
                preferenceRepository.setSelectedFont(Constants.DEFAULT_FONT_MARKER)
            }
        }
    }

    suspend fun selectFont(fontPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 可以在这里添加校验，检查 fontPath 是否在可用列表中
            preferenceRepository.setSelectedFont(fontPath)
        }
    }
}
