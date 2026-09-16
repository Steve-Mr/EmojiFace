package top.maary.emojiface.domain.model.occlusion

sealed interface OcclusionMode {
    data class Emoji(
        val emoji: String,
        val fontPath: String = ""
    ) : OcclusionMode

    data class Blur(
        val type: BlurType = BlurType.Gaussian,
        val target: BlurTarget = BlurTarget.Face
    ) : OcclusionMode {
        enum class BlurType { Gaussian, Pixelated, Halftone }
        enum class BlurTarget { Face, Eyes }
    }

    data class Avatar(
        val avatarId: String,
        val cropShape: AvatarCropShape = AvatarCropShape.Circle,
        val cornerRadiusRatio: Float = 0.2f
    ) : OcclusionMode {
        enum class AvatarCropShape { Circle, RoundedRect, Square }
    }
}
