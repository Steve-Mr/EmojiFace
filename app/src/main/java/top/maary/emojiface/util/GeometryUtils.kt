package top.maary.emojiface.util

import android.graphics.PointF
import top.maary.emojiface.ui.edit.model.BlurRegion
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * 判断一个点是否在一个旋转后的椭圆区域内部。
 *
 * @param point The point to check (tap location), 坐标基于原始图片。
 * @param region The BlurRegion defining the rotated ellipse, 坐标和角度也基于原始图片。
 * @return 如果点在内部则返回 true，否则返回 false。
 */
fun isPointInRotatedEllipse(point: PointF, region: BlurRegion): Boolean {
    // 椭圆中心
    val centerX = region.rect.centerX()
    val centerY = region.rect.centerY()

    // 椭圆的半长轴和半短轴
    val a = region.rect.width() / 2f
    val b = region.rect.height() / 2f

    // 如果半轴为0或负数，无法构成有效的椭圆
    if (a <= 0 || b <= 0) return false

    // 步骤1: 将坐标系原点平移到椭圆中心
    val translatedX = point.x - centerX
    val translatedY = point.y - centerY

    // 步骤2: 将该点进行“反向旋转”，使其回到椭圆未旋转时的标准坐标系中
    val angleRad = Math.toRadians(-region.angle.toDouble()).toFloat()
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)

    val rotatedX = translatedX * cosAngle - translatedY * sinAngle
    val rotatedY = translatedX * sinAngle + translatedY * cosAngle

    // 步骤3: 使用标准椭圆方程 (x²/a² + y²/b² <= 1) 判断该点是否在椭圆内部
    return (rotatedX.pow(2) / a.pow(2)) + (rotatedY.pow(2) / b.pow(2)) <= 1
}