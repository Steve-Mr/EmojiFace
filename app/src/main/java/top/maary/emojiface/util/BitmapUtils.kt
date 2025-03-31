package top.maary.emojiface.util

import android.graphics.Bitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

// 缩放函数
fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
    val maxSize = 1024 // 设置最大边长阈值
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxSize && height <= maxSize) {
        return bitmap
    }

    val scaleFactor = if (width > height) {
        maxSize.toFloat() / width
    } else {
        maxSize.toFloat() / height
    }

    val scaledWidth = (width * scaleFactor).toInt()
    val scaledHeight = (height * scaleFactor).toInt()

    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
    val outputStream = ByteArrayOutputStream()
    bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    return ByteArrayInputStream(outputStream.toByteArray())
}