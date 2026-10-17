package top.maary.emojiface.domain.rendering.strategies

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.withRotation
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.domain.rendering.CanvasOcclusionStrategy
import top.maary.emojiface.domain.rendering.RenderContext
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getTypeFaceFromPath
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmojiCanvasStrategy @Inject constructor() : CanvasOcclusionStrategy<OcclusionMode.Emoji> {

    override fun draw(
        canvas: Canvas,
        geometry: FaceGeometry,
        mode: OcclusionMode.Emoji,
        context: RenderContext
    ) {
        val centerX = geometry.center.x * context.coordinateScale
        val centerY = geometry.center.y * context.coordinateScale
        val diameter = geometry.effectiveSize.maxDimension * context.coordinateScale

        val typeface: Typeface? = when {
            mode.fontPath.isEmpty() || mode.fontPath == Constants.DEFAULT_FONT_MARKER -> {
                context.defaultTypeface
            }
            context.loadedTypefaces.containsKey(mode.fontPath) -> {
                context.loadedTypefaces[mode.fontPath]
            }
            else -> {
                getTypeFaceFromPath(mode.fontPath) ?: context.defaultTypeface
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            textSize = diameter
        }

        canvas.withRotation(geometry.rotationDegrees, centerX, centerY) {
            val verticalOffset = (paint.descent() + paint.ascent()) / 2f
            drawText(mode.emoji, centerX, centerY - verticalOffset, paint)
        }
    }
}
