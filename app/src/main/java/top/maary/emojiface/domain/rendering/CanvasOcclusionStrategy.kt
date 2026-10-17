package top.maary.emojiface.domain.rendering

import android.graphics.Canvas
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.occlusion.OcclusionMode

interface CanvasOcclusionStrategy<T : OcclusionMode> {
    fun draw(canvas: Canvas, geometry: FaceGeometry, mode: T, context: RenderContext)
}
