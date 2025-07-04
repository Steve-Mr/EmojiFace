package top.maary.emojiface.ui.edit.model

import android.graphics.RectF

/**
 * 用于描述模糊处理区域的数据类。
 *
 * @param rect 模糊区域的矩形边界框，基于原始图片坐标系。
 * @param angle 模糊区域需要旋转的角度。
 */
data class BlurRegion(
    val rect: RectF,
    val angle: Float,
    val originalRect: RectF = rect
)