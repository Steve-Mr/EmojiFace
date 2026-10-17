package top.maary.emojiface.domain.rendering.strategies

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.withSave
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.domain.rendering.CanvasOcclusionStrategy
import top.maary.emojiface.domain.rendering.RenderContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class AvatarCanvasStrategy @Inject constructor() : CanvasOcclusionStrategy<OcclusionMode.Avatar> {

    override fun draw(
        canvas: Canvas,
        geometry: FaceGeometry,
        mode: OcclusionMode.Avatar,
        context: RenderContext
    ) {
        val avatarBitmap = context.cachedAvatarBitmaps[mode.avatarId] ?: return

        val scale = context.coordinateScale
        val centerX = geometry.center.x * scale
        val centerY = geometry.center.y * scale
        val diameter = geometry.effectiveSize.maxDimension * scale
        val halfDim = diameter / 2f

        if (halfDim <= 0f) return

        val destRect = RectF(
            centerX - halfDim,
            centerY - halfDim,
            centerX + halfDim,
            centerY + halfDim
        )

        // 1:1 minimal-loss center-crop from source avatar bitmap
        val srcDim = min(avatarBitmap.width, avatarBitmap.height)
        if (srcDim <= 0) return
        val srcLeft = (avatarBitmap.width - srcDim) / 2
        val srcTop = (avatarBitmap.height - srcDim) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + srcDim, srcTop + srcDim)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        canvas.withSave {
            rotate(geometry.rotationDegrees, centerX, centerY)
            val clipPath = Path().apply {
                when (mode.cropShape) {
                    OcclusionMode.Avatar.AvatarCropShape.Circle -> {
                        addOval(destRect, Path.Direction.CW)
                    }
                    OcclusionMode.Avatar.AvatarCropShape.RoundedRect -> {
                        val cornerRadius = destRect.width() * mode.cornerRadiusRatio
                        addRoundRect(destRect, cornerRadius, cornerRadius, Path.Direction.CW)
                    }
                    OcclusionMode.Avatar.AvatarCropShape.Square -> {
                        addRect(destRect, Path.Direction.CW)
                    }
                }
            }
            clipPath(clipPath)
            drawBitmap(avatarBitmap, srcRect, destRect, paint)
        }
    }
}
