package top.maary.emojiface.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

/**
 * Applies a Stack Blur to a Bitmap.
 *
 * @param bitmap The Bitmap to blur.
 * @param radius The blur radius (e.g., 25).
 * @return A new blurred Bitmap.
 */
fun applyStackBlur(bitmap: Bitmap, radius: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(w.coerceAtLeast(h))

    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }

    yi = 0
    yw = yi

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    y = 0
    while (y < h) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        i = -radius
        while (i <= radius) {
            p = pix[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            i++
        }
        stackpointer = radius
        x = 0
        while (x < w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (y == 0) {
                vmin[x] = (x + radius + 1).coerceAtMost(wm)
            }
            p = pix[yw + vmin[x]]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi++
            x++
        }
        yw += w
        y++
    }
    x = 0
    while (x < w) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * w
        i = -radius
        while (i <= radius) {
            yi = 0.coerceAtLeast(yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
            i++
        }
        yi = x
        stackpointer = radius
        y = 0
        while (y < h) {
            pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = (y + r1).coerceAtMost(hm) * w
            }
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
            y++
        }
        x++
    }

    val blurredBitmap = createBitmap(w, h)
    blurredBitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return blurredBitmap
}

/**
 * 从源位图中裁剪指定区域，应用模糊效果，并返回处理后的小块位图。
 * 这个函数集中了模糊处理的所有逻辑，以确保各处效果一致。
 *
 * @param sourceBitmap 原始的、未经修改的完整位图。
 * @param regionRectF 需要模糊的区域，坐标基于 sourceBitmap。
 * @return 一个只包含模糊后区域的新的、小尺寸的位图。
 */
fun createBlurredRegionBitmap(sourceBitmap: Bitmap, regionRectF: RectF): Bitmap {
    // 1. 确保裁剪区域不会超出源位图的边界
    val cropRect = Rect(
        max(0f, regionRectF.left).toInt(),
        max(0f, regionRectF.top).toInt(),
        min(sourceBitmap.width.toFloat(), regionRectF.right).toInt(),
        min(sourceBitmap.height.toFloat(), regionRectF.bottom).toInt()
    )

    // 如果裁剪区域无效（例如宽度或高度为0），则返回一个1x1的空白位图以避免崩溃
    if (cropRect.width() <= 0 || cropRect.height() <= 0) {
        return createBitmap(1, 1)
    }

    // 2. 从源位图中裁剪出人脸区域
    val faceBitmap = Bitmap.createBitmap(
        sourceBitmap,
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height()
    )

    // 3. 根据裁剪区域的尺寸计算模糊半径。这是统一的计算标准。
    val blurRadius = (max(cropRect.width(), cropRect.height()) / 8f).toInt().coerceIn(20, 55)

    // 4. 应用模糊处理
    val blurredBitmap = applyStackBlur(faceBitmap, blurRadius)

    // 5. 释放中间创建的位图以节省内存
    faceBitmap.recycle()

    // 6. 返回最终的模糊后的小块位图
    return blurredBitmap
}
