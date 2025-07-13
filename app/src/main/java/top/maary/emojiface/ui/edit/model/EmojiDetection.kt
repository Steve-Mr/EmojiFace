package top.maary.emojiface.ui.edit.model

import kotlin.random.Random

data class EmojiDetection(
    val id: Long = (System.currentTimeMillis() shl 20) or (Random.nextLong(0, 1L shl 20)),
    val xCenter: Float,
    val yCenter: Float,
    val diameter: Float,
    val angle: Float,
    val emoji: String,
    val originalDiameter: Float = diameter
)