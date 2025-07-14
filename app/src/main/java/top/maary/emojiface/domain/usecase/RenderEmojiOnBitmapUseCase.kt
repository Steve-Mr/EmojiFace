package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.withRotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getTypeFaceFromPath
import javax.inject.Inject

class RenderEmojiOnBitmapUseCase @Inject constructor() { // 字体加载工具可以不注入，直接调用

    suspend operator fun invoke(
        baseBitmap: Bitmap,
        emojiDetections: List<EmojiDetection>,
        selectedFontPath: String // 传入选中的字体路径
    ): Result<Bitmap> = withContext(Dispatchers.Default) { // 绘制可能耗时
        runCatching {
            val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK // 或者根据需要调整
                textAlign = Paint.Align.CENTER
            }

            // 加载字体 (如果路径是默认标记，则使用系统默认字体)
            val typeface: Typeface? = if (selectedFontPath == Constants.DEFAULT_FONT_MARKER) {
                Typeface.DEFAULT
            } else {
                getTypeFaceFromPath(selectedFontPath)
            }

            emojiDetections.forEach { ed ->
                drawEmoji(canvas, ed.xCenter, ed.yCenter, ed.diameter, ed.angle, ed.emoji, paint, typeface)
            }
            mutableBitmap
        }
    }

    // 从 ViewModel 移过来的绘制单个 Emoji 的辅助函数
    private fun drawEmoji(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        diameter: Float,
        rotationAngle: Float,
        emoji: String,
        paint: Paint,
        typeface: Typeface?
    ) {
        paint.textSize = diameter
        paint.typeface = typeface // 设置字体
        canvas.withRotation(rotationAngle, centerX, centerY) {
            // 调整 baseline 使 emoji 垂直居中
            val verticalOffset = (paint.descent() + paint.ascent()) / 2
            drawText(emoji, centerX, centerY - verticalOffset, paint)
        }
    }
}
