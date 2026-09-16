package top.maary.emojiface.domain.model.occlusion

import top.maary.emojiface.domain.model.geometry.FaceGeometry
import java.util.UUID

data class FaceOcclusion(
    val id: Long = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
    val geometry: FaceGeometry,
    val mode: OcclusionMode,
    val zIndex: Int = 0,
    val isCustomized: Boolean = false
) {
    fun withMode(newMode: OcclusionMode): FaceOcclusion = copy(mode = newMode)
}
