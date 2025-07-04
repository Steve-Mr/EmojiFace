package top.maary.emojiface.domain.usecase

sealed class BlurType {
    data object Gaussian : BlurType()
    // data object Pixelated : BlurType() // for future extension
}