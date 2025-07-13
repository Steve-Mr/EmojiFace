package top.maary.emojiface.ui.edit.model

import android.graphics.RectF

/**
 * 用于描述彩蛋生成的假识别框的数据类
 * @param box 边界框在原始图片坐标系中的位置和大小 (left, top, right, bottom)
 * @param label 标签，例如 "face"
 * @param confidence 置信度，例如 0.98f
 */
data class FakeDetection(
    val box: RectF,
    val label: String,
    val confidence: Float,
    val startAge: Int,
    val endAge: Int
)