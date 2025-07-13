package top.maary.emojiface.ui.edit.state

import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.model.FakeDetection

/**
 * 封裝 Edit Screen UI 渲染所需的所有狀態。
 *
 * @param displayedBitmap 當前應顯示在主區域的 Bitmap (可能是處理後的結果，也可能是原圖)。
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
    val displayedBitmap: ImageBitmap?, // 修改：这是唯一用于显示的位图
    val aspectRatio: Float?,            // currentImage?.let { it.width.toFloat() / it.height.toFloat() }
    val emojiDetections: List<EmojiDetection>,
    val predefinedEmojiList: List<String>?,
    val fontFamily: FontFamily?,
    val typeface: Typeface?,
    val isAddMode: Boolean,
    val isProcessing: Boolean, // 或者 isAnimating，用於控制 GlowingCard 動畫
    val imageContainerSize: IntSize,
    val isAppIconHidden: Boolean,
    val availableFontNames: List<String>?, // 從 fontList (路徑) 映射過來的顯示名稱
    val selectedFontIndex: Int, // 當前選中字體在列表中的索引，方便 Dropdown 使用
    val editingEmojiIndex: Int?,
    val editingBlurRegionIndex: Int?,
    val isEasterEggEnabled: Boolean = false,
    val isTooDeep: Boolean = false,
    val fakeDetections: List<FakeDetection> = emptyList(), // 存储假识别框
    val mosaicMode: Int, // 马赛克模式状态
    val mosaicType: Int,
    val mosaicTarget: Int,
    val blurRegions: List<BlurRegion>,
    val isSliding: Boolean
)