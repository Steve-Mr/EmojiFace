package top.maary.emojiface.domain.rendering.strategies

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withSave
import top.maary.emojiface.domain.model.geometry.AnchorTarget
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.domain.rendering.CanvasOcclusionStrategy
import top.maary.emojiface.domain.rendering.RenderContext
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.util.createBlurredRegionBitmap
import top.maary.emojiface.util.createHalftoneRegionBitmap
import top.maary.emojiface.util.createPixelatedRegionBitmap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class BlurCanvasStrategy @Inject constructor() : CanvasOcclusionStrategy<OcclusionMode.Blur> {

    override fun draw(
        canvas: Canvas,
        geometry: FaceGeometry,
        mode: OcclusionMode.Blur,
        context: RenderContext
    ) {
        val regionRect = if (mode.target == OcclusionMode.Blur.BlurTarget.Eyes && geometry.anchorTarget != AnchorTarget.EYES) {
            // Derive eye region from face geometry if not already eye-targeted
            val eff = geometry.effectiveSize
            val eyeWidth = eff.width * 0.75f
            val eyeHeight = eyeWidth * 0.35f
            val rad = Math.toRadians(geometry.rotationDegrees.toDouble())
            val offsetDist = -eff.height * 0.1f
            val eyeCenterX = geometry.center.x + (offsetDist * -sin(rad)).toFloat()
            val eyeCenterY = geometry.center.y + (offsetDist * cos(rad)).toFloat()
            RectF(
                eyeCenterX - eyeWidth / 2f,
                eyeCenterY - eyeHeight / 2f,
                eyeCenterX + eyeWidth / 2f,
                eyeCenterY + eyeHeight / 2f
            )
        } else {
            geometry.toRectF()
        }

        if (regionRect.width() <= 0f || regionRect.height() <= 0f) {
            return
        }

        val blurRegion = BlurRegion(rect = regionRect, angle = geometry.rotationDegrees)

        val blurredBitmap = when (mode.type) {
            OcclusionMode.Blur.BlurType.Gaussian -> createBlurredRegionBitmap(context.baseBitmap, blurRegion)
            OcclusionMode.Blur.BlurType.Pixelated -> createPixelatedRegionBitmap(context.baseBitmap, blurRegion)
            OcclusionMode.Blur.BlurType.Halftone -> createHalftoneRegionBitmap(context.baseBitmap, blurRegion)
        }

        val scale = context.coordinateScale
        val scaledRect = RectF(
            regionRect.left * scale,
            regionRect.top * scale,
            regionRect.right * scale,
            regionRect.bottom * scale
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.withSave {
            rotate(blurRegion.angle, scaledRect.centerX(), scaledRect.centerY())
            val ovalPath = Path().apply {
                addOval(scaledRect, Path.Direction.CW)
            }
            clipPath(ovalPath)
            drawBitmap(blurredBitmap, null, scaledRect, paint)
        }

        blurredBitmap.recycle()
    }
}
