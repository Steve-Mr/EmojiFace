package top.maary.emojiface.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import top.maary.emojiface.EmojiDetection
import top.maary.emojiface.EmojiViewModel

@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel(),
               windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass) {
    EditScreenContentInternal(emojiViewModel, windowSizeClass)

}

/**
 * 封裝 Edit Screen UI 渲染所需的所有狀態。
 *
 * @param displayedBitmap 當前應顯示在主區域的 Bitmap (可能是處理後的結果，也可能是原圖)。
 * @param currentImage 原始選擇的圖片，用於獲取寬高比等。
 * @param aspectRatio 原始圖片的寬高比，用於維持圖片顯示比例。
 * @param emojiDetections 偵測到的或使用者手動添加的表情符號列表。
 * @param predefinedEmojiList 可供選擇的預定義表情符號列表。
 * @param fontFamily 當前選擇的字體 Family，用於渲染表情符號。
 * @param isAddMode 是否處於「點擊圖片新增表情符號」的模式。
 * @param isProcessing 或 isAnimating 是否正在處理圖片或顯示動畫（例如，GlowingCard 的動畫）。
 * @param imageContainerSize 圖片顯示區域的實際尺寸（用於某些計算）。
 * @param isAppIconHidden 「隱藏應用圖標」開關的狀態。
 * @param availableFontNames 可用字體的顯示名稱列表 (用於下拉選單)。
 * @param selectedFontIndex 當前選中字體在 availableFontNames 中的索引。
 */
data class EditScreenState(
    val displayedBitmap: ImageBitmap?, // resultBitmap ?: currentImage Bitmap
    val currentImage: ImageBitmap?,     // Needed for aspect ratio if displayedBitmap is null initially
    val aspectRatio: Float?,            // currentImage?.let { it.width.toFloat() / it.height.toFloat() }
    val emojiDetections: List<EmojiDetection>,
    val predefinedEmojiList: List<String>?,
    val fontFamily: FontFamily?,
    val isAddMode: Boolean,
    val isProcessing: Boolean, // 或者 isAnimating，用於控制 GlowingCard 動畫
    val imageContainerSize: IntSize,
    val isAppIconHidden: Boolean,
    val availableFontNames: List<String>?, // 從 fontList (路徑) 映射過來的顯示名稱
    val selectedFontIndex: Int, // 當前選中字體在列表中的索引，方便 Dropdown 使用
    val isMediumLayout: Boolean
)

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
    val onAddEmojiCardClick: () -> Unit,

    // --- 主要導覽和動作按鈕 ---
    /** 當使用者點擊「關閉/退出」按鈕時呼叫。 */
    val onCloseClick: () -> Unit,
    /** 當使用者點擊「分享」按鈕時呼叫。 */
    val onShareClick: () -> Unit,
    /** 當使用者點擊「儲存」按鈕時呼叫。 */
    val onSaveClick: () -> Unit,
    /** 當使用者點擊「設定」按鈕時呼叫 (通常用於打開底部工作表)。 */
    val onSettingsClick: () -> Unit,

    // --- 編輯/新增表情符號對話框操作 ---
    /** 當編輯/新增對話框確認時呼叫，傳遞使用者輸入的值。*/
    // 注意：內部邏輯會根據之前是點擊了現有卡片還是點擊了圖片來決定是更新還是新增。
    val onEditDialogConfirm: (newEmoji: String, newDiameter: Float, newRotation: Float) -> Unit,
    /** 當編輯/新增對話框被關閉時呼叫。 */
    val onEditDialogDismiss: () -> Unit,

    // --- 設定底部工作表操作 ---
    /** 當設定底部工作表被關閉時呼叫。 */
    val onSettingsSheetDismiss: () -> Unit,
    /** 當使用者在底部工作表點擊「編輯預定義表情」時呼叫。 */
    val onEditPredefinedEmojisClick: () -> Unit,
    /** 當使用者在底部工作表確認預定義表情編輯時呼叫。 */
    val onPredefinedEmojisEdited: (newEmojiListString: String) -> Unit,
    /** 當「隱藏應用圖標」開關狀態改變時呼叫。 */
    val onHideIconToggle: (hide: Boolean) -> Unit,
    /** 當使用者從下拉選單選擇了不同的字體時呼叫，傳遞選中項的索引。 */
    val onFontSelected: (index: Int) -> Unit,
    /** 當使用者點擊「新增字體」按鈕時呼叫 (觸發文件選擇器)。 */
    val onAddFontClick: () -> Unit,
    /** 當使用者點擊移除字體圖標時呼叫，傳遞要移除字體的索引。 */
    val onRemoveFontClick: (index: Int) -> Unit
)