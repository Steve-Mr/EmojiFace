package top.maary.emojiface.ui.edit.model

data class EmojiDetection(
    val xCenter: Float,
    val yCenter: Float,
    val diameter: Float,
    val angle: Float,
    val emoji: String
)