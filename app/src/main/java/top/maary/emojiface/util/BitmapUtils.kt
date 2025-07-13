package top.maary.emojiface.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import top.maary.emojiface.ui.edit.model.BlurRegion
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.max

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

    return bitmap.scale(scaledWidth, scaledHeight)
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
    var yw: Int = yi

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

fun calculateHybridCellSize(
    region: BlurRegion,
    source: Bitmap,
    baseDivisor: Float = 80f,
    regionWeight: Float = 0.8f,
    baseCells: Int = 12
): Int {
    val globalSize = (max(source.width, source.height) / baseDivisor).toInt()
    val regionSize = max(region.rect.width(), region.rect.height())
    val regionCellSize = (regionSize / baseCells).toInt()
    val hybrid = (regionCellSize * regionWeight + globalSize * (1 - regionWeight)).toInt()
    return max(hybrid, 1) // ✅ 只保证合法，不限制上界
}




/**
 * 从源位图中裁剪指定区域，应用模糊效果，并返回处理后的小块位图。
 * 这个函数集中了模糊处理的所有逻辑，以确保各处效果一致。
 *
 * @param sourceBitmap 原始的、未经修改的完整位图。
 * @return 一个只包含模糊后区域的新的、小尺寸的位图。
 */
fun createBlurredRegionBitmap(sourceBitmap: Bitmap, region: BlurRegion): Bitmap {
    val rect = region.rect
    // 1. 确保裁剪区域有效
    if (rect.width() <= 0 || rect.height() <= 0) {
        return createBitmap(1, 1)
    }

    // 2. 创建一个中间位图，用于存放从原图中提取的、未经模糊的、但已旋转对齐的内容
    val unblurredChunk = createBitmap(
        rect.width().toInt(),
        rect.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(unblurredChunk)

    // 3. 对画布进行逆向变换。我们旋转 -angle 度，以便从 sourceBitmap 中“正向”地提取内容
    canvas.rotate(-region.angle, rect.width() / 2f, rect.height() / 2f)
    // 将画布平移，使得原图中的 rect 区域的左上角与我们画布的(0,0)对齐
    canvas.translate(-rect.left, -rect.top)

    // 4. 将完整的原图绘制到经过变换的画布上。
    // 此时，只有我们感兴趣的旋转区域被绘制到了 unblurredChunk 上。
    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

    // 5. 根据区域尺寸计算模糊半径，并对提取出的内容进行模糊
    val blurRadius = calculateHybridCellSize(
        region = region,
        source = sourceBitmap,
        baseDivisor = 120f,
        baseCells = 10,
    )
    val blurredChunk = applyStackBlur(unblurredChunk, blurRadius)

    // 6. 释放中间创建的位图内存
    unblurredChunk.recycle()

    // 7. 返回最终的、内容已经旋转好并模糊过的位图
    return blurredChunk
}

/**
 * 从源位图中裁剪指定区域，应用【像素化】效果，并返回处理后的小块位图。
 *
 * @param sourceBitmap 原始的、未经修改的完整位图。
 * @param region 需要处理的区域，包含位置、大小和角度信息。
 * @return 一个只包含像素化后区域的新的、小尺寸的位图。
 */
fun createPixelatedRegionBitmap(sourceBitmap: Bitmap, region: BlurRegion): Bitmap {
    // 步骤 1: 像高斯模糊一样，先提取出旋转对齐的、未经处理的区域内容。
    // 这部分代码与 createBlurredRegionBitmap 完全一致，确保了逻辑的统一。
    val rect = region.rect
    if (rect.width() <= 0 || rect.height() <= 0) return createBitmap(1, 1)

    val unblurredChunk = createBitmap(rect.width().toInt(), rect.height().toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(unblurredChunk)
    canvas.rotate(-region.angle, rect.width() / 2f, rect.height() / 2f)
    canvas.translate(-rect.left, -rect.top)
    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

    // 步骤 2: 对提取出的内容进行像素化处理。
    // 根据区域大小决定像素块的大小，确保效果协调。尺寸越大，像素块也越大。
    val pixelSize = calculateHybridCellSize(
        region = region,
        source = sourceBitmap,
        baseDivisor = 80f,
        baseCells = 12,
    )


    // 2a. 极度缩小图片，尺寸为原始尺寸除以像素块大小。
    val tinyBitmap = unblurredChunk.scale(
        (unblurredChunk.width / pixelSize).coerceAtLeast(1),
        (unblurredChunk.height / pixelSize).coerceAtLeast(1),
        filter = false // 使用 filter = false (最近邻插值) 是像素化效果的关键
    )

    // 2b. 将极小的图片再放大回原始尺寸，同样使用最近邻插值。
    val pixelatedChunk = tinyBitmap.scale(unblurredChunk.width, unblurredChunk.height, filter = false)

    // 步骤 3: 释放中间创建的位图内存。
    unblurredChunk.recycle()
    tinyBitmap.recycle()

    // 步骤 4: 返回最终的、内容已经像素化过的位图。
    return pixelatedChunk
}

/**
 * 从源位图中裁剪指定区域，应用【半色调网点】效果，并返回处理后的小块位图。
 *
 * @param sourceBitmap 原始的、未经修改的完整位图。
 * @param region 需要处理的区域，包含位置、大小和角度信息。
 * @return 一个只包含半色调效果区域的新的、小尺寸的位图。
 */
fun createHalftoneRegionBitmap(sourceBitmap: Bitmap, region: BlurRegion): Bitmap {
    // 步骤 1: 提取旋转对齐的、未经处理的区域内容 (逻辑不变)。
    val rect = region.rect
    if (rect.width() <= 0 || rect.height() <= 0) return createBitmap(1, 1)

    val unblurredChunk = createBitmap(rect.width().toInt(), rect.height().toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(unblurredChunk)
    canvas.rotate(-region.angle, rect.width() / 2f, rect.height() / 2f)
    canvas.translate(-rect.left, -rect.top)
    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

    // 步骤 2: 创建一个临时位图，用于绘制由平均色组成的“色块背景”。
    val blockyBitmap = createBitmap(unblurredChunk.width, unblurredChunk.height, Bitmap.Config.ARGB_8888)
    val blockyCanvas = Canvas(blockyBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG) // 复用画笔
    val cellSize = calculateHybridCellSize(
        region = region,
        source = sourceBitmap,
        baseDivisor = 70f,
        baseCells = 12,
    )


    // 步骤 3: 遍历并填充“色块背景” (逻辑基本不变)。
    for (y in 0 until unblurredChunk.height step cellSize) {
        for (x in 0 until unblurredChunk.width step cellSize) {
            // --- 计算单元格内的平均颜色 (逻辑不变) ---
            var totalRed = 0L; var totalGreen = 0L; var totalBlue = 0L; var pixelCount = 0
            val endX = (x + cellSize).coerceAtMost(unblurredChunk.width)
            val endY = (y + cellSize).coerceAtMost(unblurredChunk.height)
            for (py in y until endY) {
                for (px in x until endX) {
                    val pixel = unblurredChunk[px, py]
                    totalRed += Color.red(pixel); totalGreen += Color.green(pixel); totalBlue += Color.blue(pixel)
                    pixelCount++
                }
            }
            if (pixelCount == 0) continue
            val avgColor = Color.rgb((totalRed / pixelCount).toInt(), (totalGreen / pixelCount).toInt(), (totalBlue / pixelCount).toInt())

            // 用平均色填充矩形
            paint.color = avgColor
            paint.style = Paint.Style.FILL
            blockyCanvas.drawRect(x.toFloat(), y.toFloat(), (x + cellSize).toFloat(), (y + cellSize).toFloat(), paint)
        }
    }

    // **--- 步骤 4:【核心新增】对整个“色块背景”应用一次模糊，实现平滑过渡 ---**
    // 模糊半径与单元格大小成正比，效果更佳
    val blurRadius = (cellSize * 0.7f).toInt().coerceAtLeast(1)
    val finalBitmap = applyStackBlur(blockyBitmap, blurRadius)
    // 此时 finalBitmap 已经是我们想要的、色彩平滑过渡的背景了
    blockyBitmap.recycle() // 释放临时的色块位图

    // 步骤 5: 在这个平滑的背景之上，再绘制质感圆点。
    val finalCanvas = Canvas(finalBitmap)
    // (此处重用之前的循环和计算逻辑，但只用于获取信息，并绘制圆点)
    for (y in 0 until unblurredChunk.height step cellSize) {
        val isOffsetRow = ((y / cellSize) % 2 == 1)
        for (x in 0 until unblurredChunk.width step cellSize) {
            // --- 重新计算平均颜色和亮度，仅为获取圆点信息 ---
            var totalRed = 0L; var totalGreen = 0L; var totalBlue = 0L; var pixelCount = 0
            val endX = (x + cellSize).coerceAtMost(unblurredChunk.width)
            val endY = (y + cellSize).coerceAtMost(unblurredChunk.height)
            for (py in y until endY) {
                for (px in x until endX) {
                    val pixel = unblurredChunk[px, py]
                    totalRed += Color.red(pixel); totalGreen += Color.green(pixel); totalBlue += Color.blue(pixel)
                    pixelCount++
                }
            }
            if (pixelCount == 0) continue
            val avgRed = (totalRed / pixelCount).toInt(); val avgGreen = (totalGreen / pixelCount).toInt(); val avgBlue = (totalBlue / pixelCount).toInt()

            // 绘制圆点逻辑 (与您提供的代码一致)
            val brightness = (avgRed * 0.299 + avgGreen * 0.587 + avgBlue * 0.114) / 255.0
            val radius = (brightness * cellSize * 0.5).toFloat()
            val darkerColor = Color.rgb((avgRed * 0.8).toInt(), (avgGreen * 0.8).toInt(), (avgBlue * 0.8).toInt())
            paint.color = darkerColor

            val cx = x + cellSize / 2f + if (isOffsetRow) cellSize / 2f else 0f
            val cy = y + cellSize / 2f
            finalCanvas.drawCircle(cx, cy, radius, paint)
        }
    }

    // 步骤 6: 释放中间位图 (逻辑不变)。
    unblurredChunk.recycle()

    // 步骤 7: 返回最终效果。
    return finalBitmap
}


