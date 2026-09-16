package top.maary.emojiface.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AvatarTest {

    @Test
    fun resolveFile_resolvesRelativePathAgainstFilesDir() {
        val dummyFilesDir = File("/data/user/0/top.maary.emojiface/files")
        val avatar = Avatar(
            id = "avatar_uuid_123",
            fileName = "avatar_1700000000000_uuid.png",
            relativePath = "avatars/avatar_1700000000000_uuid.png",
            displayName = "Custom Avatar",
            dateAdded = 1700000000000L,
            width = 1080,
            height = 1080,
            mimeType = "image/png",
            sizeBytes = 250_000L
        )

        val resolved = avatar.resolveFile(dummyFilesDir)
        assertEquals(File(dummyFilesDir, "avatars/avatar_1700000000000_uuid.png"), resolved)
        assertEquals("/data/user/0/top.maary.emojiface/files/avatars/avatar_1700000000000_uuid.png", resolved.path)
    }

    @Test
    fun calculateSquareCrop_wideImage_centersHorizontally() {
        // 1920x1080 -> 1080x1080 centered at (420, 0)
        val crop = Avatar.calculateSquareCrop(rawWidth = 1920, rawHeight = 1080)
        assertEquals("Horizontal offset x must center the square", 420, crop.x)
        assertEquals("Vertical offset y must be 0 for full height square", 0, crop.y)
        assertEquals("Crop dimension must match the minimum dimension", 1080, crop.size)
    }

    @Test
    fun calculateSquareCrop_tallImage_centersVertically() {
        // 1080x1920 -> 1080x1080 centered at (0, 420)
        val crop = Avatar.calculateSquareCrop(rawWidth = 1080, rawHeight = 1920)
        assertEquals("Horizontal offset x must be 0 for full width square", 0, crop.x)
        assertEquals("Vertical offset y must center the square", 420, crop.y)
        assertEquals("Crop dimension must match the minimum dimension", 1080, crop.size)
    }

    @Test
    fun calculateSquareCrop_squareImage_zeroOffset() {
        // 1000x1000 -> 1000x1000 centered at (0, 0)
        val crop = Avatar.calculateSquareCrop(rawWidth = 1000, rawHeight = 1000)
        assertEquals("Horizontal offset x must be 0", 0, crop.x)
        assertEquals("Vertical offset y must be 0", 0, crop.y)
        assertEquals("Crop dimension must equal the square dimension", 1000, crop.size)
    }

    @Test
    fun calculateSquareCrop_oddDimensions_handlesEvenIntegerDivision() {
        val crop = Avatar.calculateSquareCrop(rawWidth = 1001, rawHeight = 500)
        // (1001 - 500) / 2 = 501 / 2 = 250
        assertEquals(250, crop.x)
        assertEquals(0, crop.y)
        assertEquals(500, crop.size)
    }

    @Test
    fun jsonSerialization_encodesAndDecodesAvatarListCorrectly() {
        val avatarList = listOf(
            Avatar(
                id = "avatar_1",
                fileName = "avatar_1.png",
                relativePath = "avatars/avatar_1.png",
                displayName = "Profile Photo",
                dateAdded = 1710000000000L,
                width = 512,
                height = 512,
                mimeType = "image/png",
                sizeBytes = 102400L
            ),
            Avatar(
                id = "avatar_2",
                fileName = "avatar_2.webp",
                relativePath = "avatars/avatar_2.webp",
                displayName = "Sticker Avatar",
                dateAdded = 1710000050000L,
                width = 1024,
                height = 1024,
                mimeType = "image/webp",
                sizeBytes = 204800L
            )
        )

        val jsonString = Json.encodeToString(avatarList)
        val deserialized = Json.decodeFromString<List<Avatar>>(jsonString)

        assertEquals("Deserialized list must match original list", avatarList, deserialized)
        assertEquals(2, deserialized.size)
        assertEquals("avatar_1", deserialized[0].id)
        assertEquals("Profile Photo", deserialized[0].displayName)
        assertEquals(512, deserialized[0].width)
        assertEquals("avatar_2", deserialized[1].id)
    }

    @Test
    fun jsonSerialization_emptyList_roundTrips() {
        val emptyList = emptyList<Avatar>()
        val json = Json.encodeToString(emptyList)
        assertEquals("[]", json)
        val deserialized = Json.decodeFromString<List<Avatar>>(json)
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun helperProperties_aspectRatioAndTransparency() {
        val pngAvatar = Avatar(
            id = "test_png",
            fileName = "avatar.png",
            relativePath = "avatars/avatar.png",
            displayName = "PNG Avatar",
            dateAdded = 1000L,
            width = 1536,
            height = 1536,
            mimeType = "image/png",
            sizeBytes = 5000L
        )

        assertEquals(1.0f, pngAvatar.aspectRatio, 0.0001f)
        assertTrue("PNG format must be recognized as transparent", pngAvatar.isTransparentFormat)

        val jpegAvatar = Avatar(
            id = "test_jpg",
            fileName = "avatar.jpg",
            relativePath = "avatars/avatar.jpg",
            displayName = "JPG Avatar",
            dateAdded = 1000L,
            width = 800,
            height = 800,
            mimeType = "image/jpeg",
            sizeBytes = 4000L
        )

        assertEquals(1.0f, jpegAvatar.aspectRatio, 0.0001f)
        assertFalse("JPEG format must not be recognized as transparent", jpegAvatar.isTransparentFormat)

        val webpAvatar = Avatar(
            id = "test_webp",
            fileName = "avatar.webp",
            relativePath = "avatars/avatar.webp",
            displayName = "WebP Avatar",
            dateAdded = 1000L,
            width = 1024,
            height = 1024,
            mimeType = "image/webp",
            sizeBytes = 3500L
        )

        assertTrue("WebP format must be recognized as transparent", webpAvatar.isTransparentFormat)
    }

    @Test
    fun calculateSquareCrop_zeroAndNegativeDimensions_returnsZeroSizeAndNonNegativeOffsets() {
        val cropZero = Avatar.calculateSquareCrop(rawWidth = 0, rawHeight = 1080)
        assertEquals(0, cropZero.x)
        assertEquals(540, cropZero.y)
        assertEquals(0, cropZero.size)

        val cropNegative = Avatar.calculateSquareCrop(rawWidth = -100, rawHeight = -200)
        assertEquals(0, cropNegative.x)
        assertEquals(0, cropNegative.y)
        assertEquals(0, cropNegative.size)
    }

    @Test
    fun aspectRatio_zeroOrNegativeHeight_defaultsTo1() {
        val zeroHeightAvatar = Avatar(
            id = "zero_h",
            fileName = "zero.png",
            relativePath = "avatars/zero.png",
            displayName = "Zero Height",
            dateAdded = 1000L,
            width = 500,
            height = 0,
            mimeType = "image/png",
            sizeBytes = 100L
        )
        assertEquals(1.0f, zeroHeightAvatar.aspectRatio, 0.0001f)

        val negativeHeightAvatar = zeroHeightAvatar.copy(height = -50)
        assertEquals(1.0f, negativeHeightAvatar.aspectRatio, 0.0001f)
    }

    @Test
    fun isTransparentFormat_caseInsensitiveAndExtensionFallbacks() {
        val upperPng = Avatar(
            id = "upper_png",
            fileName = "IMG_01.PNG",
            relativePath = "avatars/IMG_01.PNG",
            displayName = "Upper PNG",
            dateAdded = 1000L,
            width = 100,
            height = 100,
            mimeType = "IMAGE/PNG",
            sizeBytes = 100L
        )
        assertTrue(upperPng.isTransparentFormat)

        val unknownMimeWithWebpExt = Avatar(
            id = "unknown_mime",
            fileName = "photo.WEBP",
            relativePath = "avatars/photo.WEBP",
            displayName = "Unknown Mime WebP",
            dateAdded = 1000L,
            width = 100,
            height = 100,
            mimeType = "application/octet-stream",
            sizeBytes = 100L
        )
        assertTrue(unknownMimeWithWebpExt.isTransparentFormat)

        val unknownMimeBmp = Avatar(
            id = "bmp_avatar",
            fileName = "photo.bmp",
            relativePath = "avatars/photo.bmp",
            displayName = "BMP",
            dateAdded = 1000L,
            width = 100,
            height = 100,
            mimeType = "image/bmp",
            sizeBytes = 100L
        )
        assertFalse(unknownMimeBmp.isTransparentFormat)
    }
}
