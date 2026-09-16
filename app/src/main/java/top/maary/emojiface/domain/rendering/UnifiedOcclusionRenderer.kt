package top.maary.emojiface.domain.rendering

import android.graphics.Canvas
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.domain.rendering.strategies.AvatarCanvasStrategy
import top.maary.emojiface.domain.rendering.strategies.BlurCanvasStrategy
import top.maary.emojiface.domain.rendering.strategies.EmojiCanvasStrategy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedOcclusionRenderer @Inject constructor(
    private val emojiStrategy: EmojiCanvasStrategy,
    private val blurStrategy: BlurCanvasStrategy,
    private val avatarStrategy: AvatarCanvasStrategy
) {
    fun render(
        canvas: Canvas,
        occlusion: FaceOcclusion,
        context: RenderContext
    ) {
        when (val mode = occlusion.mode) {
            is OcclusionMode.Emoji -> emojiStrategy.draw(canvas, occlusion.geometry, mode, context)
            is OcclusionMode.Blur -> blurStrategy.draw(canvas, occlusion.geometry, mode, context)
            is OcclusionMode.Avatar -> avatarStrategy.draw(canvas, occlusion.geometry, mode, context)
        }
    }

    fun render(
        canvas: Canvas,
        occlusions: List<FaceOcclusion>,
        context: RenderContext
    ) {
        occlusions.sortedBy { it.zIndex }.forEach { occlusion ->
            render(canvas, occlusion, context)
        }
    }
}
