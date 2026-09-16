package top.maary.emojiface.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.maary.emojiface.domain.model.geometry.AnchorTarget
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.geometry.Point2D
import top.maary.emojiface.domain.model.geometry.Size2D
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode

class FaceOcclusionTest {

    private val sampleGeometry = FaceGeometry(
        center = Point2D(150f, 250f),
        baseSize = Size2D(120f, 120f),
        scaleFactor = 1.25f,
        rotationDegrees = 15f,
        anchorTarget = AnchorTarget.FACE
    )

    @Test
    fun defaultId_isPositiveAndUnique() {
        val occlusion1 = FaceOcclusion(
            geometry = sampleGeometry,
            mode = OcclusionMode.Emoji("😀")
        )
        val occlusion2 = FaceOcclusion(
            geometry = sampleGeometry,
            mode = OcclusionMode.Emoji("😀")
        )

        assertTrue("Occlusion ID should be positive", occlusion1.id >= 0)
        assertTrue("Occlusion ID should be positive", occlusion2.id >= 0)
        assertNotEquals("Each occlusion should receive a unique default ID", occlusion1.id, occlusion2.id)
    }

    @Test
    fun withMode_preservesGeometryIdAndZIndexAcrossAllModeTransitions() {
        val original = FaceOcclusion(
            id = 42L,
            geometry = sampleGeometry,
            mode = OcclusionMode.Emoji(emoji = "😎", fontPath = "/fonts/custom.ttf"),
            zIndex = 5
        )

        // 1. Transition Emoji -> Blur
        val blurMode = OcclusionMode.Blur(
            type = OcclusionMode.Blur.BlurType.Pixelated,
            target = OcclusionMode.Blur.BlurTarget.Eyes
        )
        val blurOcclusion = original.withMode(blurMode)

        assertEquals("ID must be preserved across mode changes", original.id, blurOcclusion.id)
        assertEquals("Geometry must be preserved across mode changes", original.geometry, blurOcclusion.geometry)
        assertEquals("zIndex must be preserved across mode changes", original.zIndex, blurOcclusion.zIndex)
        assertEquals("Mode should be updated to Blur", blurMode, blurOcclusion.mode)

        // 2. Transition Blur -> Avatar
        val avatarMode = OcclusionMode.Avatar(
            avatarId = "avatar_alpha_01",
            cropShape = OcclusionMode.Avatar.AvatarCropShape.RoundedRect,
            cornerRadiusRatio = 0.25f
        )
        val avatarOcclusion = blurOcclusion.withMode(avatarMode)

        assertEquals("ID must be preserved across mode changes", original.id, avatarOcclusion.id)
        assertEquals("Geometry must be preserved across mode changes", original.geometry, avatarOcclusion.geometry)
        assertEquals("zIndex must be preserved across mode changes", original.zIndex, avatarOcclusion.zIndex)
        assertEquals("Mode should be updated to Avatar", avatarMode, avatarOcclusion.mode)

        // 3. Transition Avatar -> Emoji
        val emojiMode = OcclusionMode.Emoji(emoji = "🥳")
        val finalEmojiOcclusion = avatarOcclusion.withMode(emojiMode)

        assertEquals("ID must be preserved across mode changes", original.id, finalEmojiOcclusion.id)
        assertEquals("Geometry must be preserved across mode changes", original.geometry, finalEmojiOcclusion.geometry)
        assertEquals("zIndex must be preserved across mode changes", original.zIndex, finalEmojiOcclusion.zIndex)
        assertEquals("Mode should be updated to Emoji", emojiMode, finalEmojiOcclusion.mode)
    }

    @Test
    fun zIndex_sortingOrdersCorrectly() {
        val occ1 = FaceOcclusion(geometry = sampleGeometry, mode = OcclusionMode.Emoji("1"), zIndex = 3)
        val occ2 = FaceOcclusion(geometry = sampleGeometry, mode = OcclusionMode.Emoji("2"), zIndex = 0)
        val occ3 = FaceOcclusion(geometry = sampleGeometry, mode = OcclusionMode.Emoji("3"), zIndex = 1)

        val list = listOf(occ1, occ2, occ3)
        val sorted = list.sortedBy { it.zIndex }

        assertEquals(listOf(occ2, occ3, occ1), sorted)
    }
}
