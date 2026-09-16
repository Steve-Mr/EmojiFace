package top.maary.emojiface.domain.model.geometry

import kotlin.math.max
import kotlin.math.min

data class Size2D(
    val width: Float,
    val height: Float
) {
    val maxDimension: Float get() = max(width, height)
    val minDimension: Float get() = min(width, height)

    fun scale(factor: Float): Size2D = Size2D(width * factor, height * factor)
}
