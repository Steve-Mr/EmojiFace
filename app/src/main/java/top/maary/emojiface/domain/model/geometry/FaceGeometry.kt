package top.maary.emojiface.domain.model.geometry

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

enum class AnchorTarget {
    FACE,
    EYES
}

data class FaceGeometry(
    val center: Point2D,
    val baseSize: Size2D,
    val scaleFactor: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val anchorTarget: AnchorTarget = AnchorTarget.FACE
) {
    val effectiveSize: Size2D
        get() = baseSize.scale(scaleFactor)

    fun toRectF(): RectF {
        val eff = effectiveSize
        val halfW = eff.width / 2f
        val halfH = eff.height / 2f
        return RectF(
            center.x - halfW,
            center.y - halfH,
            center.x + halfW,
            center.y + halfH
        )
    }

    /**
     * Checks if the given point is inside the rotated geometry boundary.
     *
     * @param point The test point in the same coordinate space as [center].
     * @param treatAsEllipse True to perform ellipse hit testing, false for rotated bounding box.
     */
    fun contains(point: Point2D, treatAsEllipse: Boolean = true): Boolean {
        val eff = effectiveSize
        val halfW = eff.width / 2f
        val halfH = eff.height / 2f
        if (halfW <= 0f || halfH <= 0f) return false

        // 1. Translate point to geometry center
        val translatedX = point.x - center.x
        val translatedY = point.y - center.y

        // 2. Inverse rotate by -rotationDegrees
        val angleRad = Math.toRadians(-rotationDegrees.toDouble()).toFloat()
        val cosAngle = cos(angleRad)
        val sinAngle = sin(angleRad)

        val rotatedX = translatedX * cosAngle - translatedY * sinAngle
        val rotatedY = translatedX * sinAngle + translatedY * cosAngle

        // 3. Test inside unrotated bounds
        return if (treatAsEllipse) {
            (rotatedX.pow(2) / halfW.pow(2)) + (rotatedY.pow(2) / halfH.pow(2)) <= 1f
        } else {
            abs(rotatedX) <= halfW && abs(rotatedY) <= halfH
        }
    }

    fun transform(ratio: Float): FaceGeometry = copy(
        center = Point2D(center.x * ratio, center.y * ratio),
        baseSize = Size2D(baseSize.width * ratio, baseSize.height * ratio)
    )
}
