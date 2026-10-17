package top.maary.emojiface.ui.edit.state

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import top.maary.emojiface.domain.model.occlusion.OcclusionMode

/**
 * Encapsulates all user actions and events that can be triggered from Edit Screen UI.
 */
data class EditScreenActions(
    // --- Image Area Operations ---
    val onImageTapToAdd: (tapPosition: Offset) -> Unit,
    val onImageContainerMeasured: (size: IntSize) -> Unit,
    val onPickImageClick: () -> Unit,
    val onClearImageClick: () -> Unit,

    // --- Occlusion Item & Card Operations ---
    val onAddClicked: () -> Unit,
    val onOcclusionSelected: (index: Int) -> Unit,
    val onOcclusionLongClick: (index: Int) -> Unit,
    val onDeleteOcclusion: (index: Int) -> Unit,

    // --- Main Navigation and Toolbar Actions ---
    val onCloseClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onSettingsClick: () -> Unit,

    // --- Unified Editing Operations ---
    val onOcclusionModeChange: (newMode: OcclusionMode) -> Unit,
    val onOcclusionSizeChange: (factor: Float) -> Unit,
    val onOcclusionAngleChange: (angle: Float) -> Unit,
    val onConfirmEditing: () -> Unit,
    val onCancelEditing: () -> Unit,

    // --- Avatar Management Operations ---
    val onManageAvatarsClick: () -> Unit,
    val onAvatarImported: (Uri) -> Unit,
    val onAvatarDeleted: (String) -> Unit,
    val onAvatarDefaultSet: (String) -> Unit,

    // --- Settings Operations ---
    val onSettingsSheetDismiss: () -> Unit,
    val onPredefinedEmojisEdited: (newEmojiListString: String) -> Unit,
    val onHideIconToggle: (hide: Boolean) -> Unit,
    val onFontSelected: (index: Int) -> Unit,
    val onAddFontClick: () -> Unit,
    val onRemoveFontClick: (index: Int) -> Unit,

    val onEasterEggStateChanged: (Boolean) -> Unit,
    val onTooDeepStateChanged: (Boolean) -> Unit,

    val onMosaicModeSelected: (Int) -> Unit,
    val onMosaicTypeSelected: (type: Int) -> Unit,
    val onMosaicTargetSelected: (Int) -> Unit,

    val onSlidingStateChange: (isSliding: Boolean) -> Unit,
    val onToggleEditMode: () -> Unit = {},
    val onSetEditMode: (Boolean) -> Unit = {},
    val onTriggerEditButtonShake: () -> Unit = {}
)