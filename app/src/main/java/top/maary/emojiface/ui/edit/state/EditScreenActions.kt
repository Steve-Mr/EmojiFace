package top.maary.emojiface.ui.edit.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * 封裝 Edit Screen UI 可以觸發的所有使用者操作或事件。
 */
data class EditScreenActions(
    // --- 圖片區域操作 ---
    /** 當處於 isAddMode 且使用者點擊圖片時呼叫，傳遞點擊位置。 */
    val onImageTapToAdd: (tapPosition: Offset) -> Unit,
    /** 當圖片容器佈局完成或尺寸改變時呼叫。 */
    val onImageContainerMeasured: (size: IntSize) -> Unit,
    /** 當使用者點擊「選擇圖片」按鈕或區域時呼叫。 */
    val onPickImageClick: () -> Unit,
    /** 當使用者點擊「清除圖片」按鈕時呼叫。 */
    val onClearImageClick: () -> Unit,

    // --- 表情符號列表操作 ---
    /** 當使用者點擊現有的 EmojiCard 時呼叫，傳遞其索引。 */
    val onEmojiCardClick: (index: Int) -> Unit,
    /** 當使用者點擊「新增」的 EmojiCard 時呼叫。 */
    val onAddClicked: () -> Unit,
    /** 當使用者長按現有的 EmojiCard 時呼叫，傳遞其索引以进行删除。 */
    val onEmojiCardLongClick: (index: Int) -> Unit,

    val onBlurRegionSelected: (Int) -> Unit,

    // --- 主要導覽和動作按鈕 ---
    /** 當使用者點擊「關閉/退出」按鈕時呼叫。 */
    val onCloseClick: () -> Unit,
    /** 當使用者點擊「分享」按鈕時呼叫。 */
    val onShareClick: () -> Unit,
    /** 當使用者點擊「儲存」按鈕時呼叫。 */
    val onSaveClick: () -> Unit,
    /** 當使用者點擊「設定」按鈕時呼叫 (通常用於打開底部工作表)。 */
    val onSettingsClick: () -> Unit,

    // --- 实时编辑表情符號操作 ---
    /** 当用户在实时编辑时，参数发生了变化 */
    val onEditingValueChanged: (emoji: String? , diameter: Float?, rotation: Float?) -> Unit,
    /** 当用户确认实时编辑的结果时调用 */
    val onConfirmEditing: () -> Unit,
    /** 当用户取消实时编辑时调用 */
    val onCancelEditing: () -> Unit,

    // --- 实时编辑操作 (重构后) ---
    /** 当用户在编辑时更改 Emoji 字符或从列表中选择时调用。 */
    val onEmojiChange: (emoji: String) -> Unit,
    /** 当用户在编辑时拖动大小滑块时调用，传递新的比例因子。 */
    val onSizeFactorChange: (factor: Float) -> Unit,
    /** 当用户在编辑时拖动角度滑块时调用。 */
    val onAngleChange: (angle: Float) -> Unit,

    // --- Blur 区域专属编辑操作 ---
    /** 当用户在编辑 Blur 区域时拖动大小滑块时调用。 */
    val onBlurRegionSizeChange: (factor: Float) -> Unit,
    /** 当用户在编辑 Blur 区域时拖动角度滑块时调用。 */
    val onBlurRegionAngleChange: (angle: Float) -> Unit,

    // --- 設定底部工作表操作 ---
    /** 當設定底部工作表被關閉時呼叫。 */
    val onSettingsSheetDismiss: () -> Unit,
    /** 當使用者在底部工作表確認預定義表情編輯時呼叫。 */
    val onPredefinedEmojisEdited: (newEmojiListString: String) -> Unit,
    /** 當「隱藏應用圖標」開關狀態改變時呼叫。 */
    val onHideIconToggle: (hide: Boolean) -> Unit,
    /** 當使用者從下拉選單選擇了不同的字體時呼叫，傳遞選中項的索引。 */
    val onFontSelected: (index: Int) -> Unit,
    /** 當使用者點擊「新增字體」按鈕時呼叫 (觸發文件選擇器)。 */
    val onAddFontClick: () -> Unit,
    /** 當使用者點擊移除字體圖標時呼叫，傳遞要移除字體的索引。 */
    val onRemoveFontClick: (index: Int) -> Unit,

    val onEasterEggStateChanged: (Boolean) -> Unit,
    val onTooDeepStateChanged: (Boolean) -> Unit,

    val onMosaicModeSelected: (Int) -> Unit,
    val onMosaicTypeSelected: (type: Int) -> Unit,
    val onMosaicTargetSelected: (Int) -> Unit,

    val onSlidingStateChange: (isSliding: Boolean) -> Unit
)