package top.maary.emojiface.ui.edit.state

import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.ui.edit.model.FakeDetection

/**
 * Encapsulates all state required for Edit Screen UI rendering.
 */
data class EditScreenState(
    val displayedBitmap: ImageBitmap?,
    val aspectRatio: Float?,
    val occlusions: List<FaceOcclusion> = emptyList(),
    val editingOcclusionIndex: Int? = null,
    val editingOcclusion: FaceOcclusion? = null,
    val availableAvatars: List<Avatar> = emptyList(),
    val cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    val defaultMosaicMode: Int = PreferenceRepository.MOSAIC_MODE_EMOJI,
    val predefinedEmojiList: List<String>? = null,
    val fontFamily: FontFamily? = null,
    val typeface: Typeface? = null,
    val isAddMode: Boolean = false,
    val isProcessing: Boolean = false,
    val imageContainerSize: IntSize = IntSize.Zero,
    val isAppIconHidden: Boolean = false,
    val availableFontNames: List<String>? = null,
    val selectedFontIndex: Int = 0,
    val isEasterEggEnabled: Boolean = false,
    val isTooDeep: Boolean = false,
    val fakeDetections: List<FakeDetection> = emptyList(),
    val mosaicMode: Int = PreferenceRepository.MOSAIC_MODE_EMOJI,
    val mosaicType: Int = PreferenceRepository.MOSAIC_TYPE_GAUSSIAN,
    val mosaicTarget: Int = PreferenceRepository.MOSAIC_TARGET_FACE,
    val isSliding: Boolean = false,
    val isEditMode: Boolean = false,
    val shakeEditButtonTrigger: Long = 0L
)