package top.maary.emojiface.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import java.util.Locale
import kotlin.random.Random

fun getFileNameFromUri(uri: Uri, application: Context): String {
    application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
    }
    return ""
}

fun generateShortUniqueId(): String {
    val timestamp = System.currentTimeMillis() / 1000 // 秒级时间戳
    val random = Random.nextInt(1000) // 0-999 随机数
    return String.format(Locale.getDefault().toString(), timestamp % 1000, random) // 格式化为 6 位数字
}

fun getTypeFaceFromPath(filePath: String?): Typeface? {
    if (filePath.isNullOrEmpty()) return null
    val file = File(filePath)
    if (file.exists()) {
        try {
            return Typeface.createFromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    return null
}

fun getFileNameWithoutExtensionUsingPath(filePath: String): String {
    val path = Paths.get(filePath)
    val fileName = path.fileName.toString()
    val dotIndex = fileName.lastIndexOf(".")
    return if (dotIndex == -1) {
        fileName
    } else {
        fileName.substring(0, dotIndex)
    }
}

fun copyUriToInternal(uri: Uri, application: Context) : File{
    val contentResolver = application.contentResolver

    // 从 URI 中获取文件名
    val originalFileName = getFileNameFromUri(uri = uri, application = application)

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
    val destFile = File(application.filesDir, fileName)

    contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(destFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return destFile
}

fun loadFontFromPath(filePath: String?): FontFamily? {
    if (filePath.isNullOrEmpty()) return null
    val file = File(filePath)
    if (file.exists()) {
        try {
            return FontFamily(Font(file = file))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    return null
}

fun generateBitmapUri(bitmap: Bitmap, application: Context) : Uri {
    // 保存到缓存文件
    val cachePath = File(application.cacheDir, "images").apply { mkdirs() }
    val file = File(cachePath, "shared_${System.currentTimeMillis()}.png").apply {
        FileOutputStream(this).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    // 生成安全 Uri
    return  FileProvider.getUriForFile(
        application,
        "${application.packageName}.fileprovider",
        file
    )
}

fun saveImageToFile(bitmap: Bitmap, application: Context) : String? {
    try {
        // 使用 MediaStore API 保存到公共目录
        val folderName = "FaceMoji"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "facemoji_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,  "${Environment.DIRECTORY_PICTURES}/$folderName")
        }

        // 插入 MediaStore 并获取 Uri
        val resolver = application.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("cannot create file")

        // 写入图片数据
        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw IOException("failed")
            }
        }

        // 提示成功
        return null
    } catch (e: Exception) {
        return e.message
    }
}