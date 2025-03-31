package top.maary.emojiface.util

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import kotlin.random.Random

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

