package top.maary.emojiface.domain.model.geometry

import kotlin.math.hypot

data class Point2D(
    val x: Float,
    val y: Float
) {
    fun distanceTo(other: Point2D): Float = hypot(x - other.x, y - other.y)

    operator fun plus(other: Point2D): Point2D = Point2D(x + other.x, y + other.y)

    operator fun minus(other: Point2D): Point2D = Point2D(x - other.x, y - other.y)
}
