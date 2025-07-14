package top.maary.emojiface.ui.edit // Or your chosen package

// Imports for Android, Lifecycle, Coroutines, Hilt, Flows, Graphics etc.
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_BLUR
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_EMOJI
import top.maary.emojiface.domain.usecase.BlurType
import top.maary.emojiface.domain.usecase.CalculateBlurRegionsUseCase
import top.maary.emojiface.domain.usecase.CalculateEmojiPositionsUseCase
import top.maary.emojiface.domain.usecase.DetectFacesUseCase
import top.maary.emojiface.domain.usecase.DetectionOutput
import top.maary.emojiface.domain.usecase.GenerateShareableUriUseCase
import top.maary.emojiface.domain.usecase.GetBitmapUseCase
import top.maary.emojiface.domain.usecase.ManageAppIconVisibilityUseCase
import top.maary.emojiface.domain.usecase.ManageFontUseCase
import top.maary.emojiface.domain.usecase.RenderEmojiOnBitmapUseCase
import top.maary.emojiface.domain.usecase.RenderMosaicOnBitmapUseCase
import top.maary.emojiface.domain.usecase.SaveImageUseCase
import top.maary.emojiface.domain.usecase.UpdateEmojiOptionsUseCase
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.model.FakeDetection
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getTypeFaceFromPath
import top.maary.emojiface.util.loadFontFromPath
import top.maary.emojiface.util.scaleBitmapIfNeeded
import javax.inject.Inject
import kotlin.random.Random

// Define the UI State Data Class (can be in a separate file)
data class EditUiState(
    val originalBitmap: Bitmap? = null, // 新增：始终保存高分辨率原图
    val displayedBitmap: Bitmap? = null, // 修改：这是用于UI显示和实时处理的位图（低分辨率）
    val selectedEmojis: List<EmojiDetection> = emptyList(),
    val predefinedEmojiOptions: List<String> = PreferenceRepository.DEFAULT_EMOJI_LIST, // Default
    val isAppIconHidden: Boolean = false,
    val availableFontPaths: List<String> = listOf(Constants.DEFAULT_FONT_MARKER),
    val selectedFontPath: String = Constants.DEFAULT_FONT_MARKER,
    val loadedFontFamily: FontFamily? = null,
    val loadedTypeface: Typeface? = null, // <--- 新增原生 Typeface 状态
    val isProcessing: Boolean = false, // For background tasks like detection/initial render
    val isRendering: Boolean = false, // Specific for re-rendering after updates
    val errorMessage: String? = null,
    val successMessage: String? = null, // Optional for short success feedback
    val editingEmojiIndex: Int? = null, // 正在编辑的 emoji 的索引
    val editingEmoji: EmojiDetection? = null, // 正在编辑的 emoji 的瞬时数据
    val editingBlurRegionIndex: Int? = null,
    val editingBlurRegion: BlurRegion? = null,
    val isEasterEggEnabled: Boolean = false,
    val isTooDeep: Boolean = false,
    val fakeDetections: List<FakeDetection> = emptyList(), // 存储假识别框
    val detectionOutput: DetectionOutput? = null, // 保存原始检测结果
    val mosaicMode: Int = MOSAIC_MODE_EMOJI, // 马赛克模式状态
    val mosaicType: Int = PreferenceRepository.MOSAIC_TYPE_GAUSSIAN,
    val mosaicTarget: Int = PreferenceRepository.MOSAIC_TARGET_FACE,
    val blurRegions: List<BlurRegion> = emptyList(),
    val isSliding: Boolean = false,
    val aspectRatio: Float? = null // 新增：保存原图的宽高比
)

@HiltViewModel
class EmojiViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository, // Keep for observing flows
    // Inject Use Cases
    private val detectFacesUseCase: DetectFacesUseCase,
    private val calculateEmojiPositionsUseCase: CalculateEmojiPositionsUseCase,
    private val calculateBlurRegionsUseCase: CalculateBlurRegionsUseCase,
    private val renderEmojiOnBitmapUseCase: RenderEmojiOnBitmapUseCase,
    private val manageFontUseCase: ManageFontUseCase, // Handles add/remove/select
    private val saveImageUseCase: SaveImageUseCase,
    private val generateShareableUriUseCase: GenerateShareableUriUseCase,
    private val manageAppIconVisibilityUseCase: ManageAppIconVisibilityUseCase, // Assuming created
    private val updateEmojiOptionsUseCase: UpdateEmojiOptionsUseCase, // Assuming created
    private val renderMosaicOnBitmapUseCase: RenderMosaicOnBitmapUseCase, // 注入
    private val getBitmapUseCase: GetBitmapUseCase
) : ViewModel() {

    // --- State Management ---
    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    // --- Event Channel ---
    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent.asSharedFlow()

    private val editingStateFlow = MutableStateFlow<EmojiDetection?>(null)

    private var mosaicModeUpdateJob: Job? = null

    init {
        // Observe preferences and update state
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferenceRepository.emojiOptionsFlow.collect { options ->
                _uiState.update { it.copy(predefinedEmojiOptions = options) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.isIconHide.collect { hidden ->
                _uiState.update { it.copy(isAppIconHidden = hidden) }
            }
        }
        viewModelScope.launch {
            combine(
                preferenceRepository.fontsList,
                preferenceRepository.selectedFont
            ) { paths, selectedPath ->
                // 同时加载两种字体对象
                val fontFamily = loadFontFromPath(selectedPath)
                val typeface = if (selectedPath == Constants.DEFAULT_FONT_MARKER) {
                    Typeface.DEFAULT
                } else {
                    getTypeFaceFromPath(selectedPath)
                }
                // 将所有状态打包
                Triple(paths, selectedPath, Pair(fontFamily, typeface))
            }.collect { (paths, selectedPath, fontPair) ->
                val (fontFamily, typeface) = fontPair
                _uiState.update { currentState ->
                    currentState.copy(
                        availableFontPaths = paths,
                        selectedFontPath = selectedPath,
                        loadedFontFamily = fontFamily, // 更新 Compose 字体
                        loadedTypeface = typeface      // <--- 更新原生字体
                    )
                }
            }
        }
        viewModelScope.launch {
            preferenceRepository.isEasterEggEnabled.collect { enabled ->
                _uiState.update { it.copy(isEasterEggEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.isTooDeep.collect { enabled ->
                _uiState.update { it.copy(isTooDeep = enabled) }
            }
        }
        viewModelScope.launch {
            combine(
                preferenceRepository.mosaicMode,
                preferenceRepository.mosaicTarget
            ) { mode, target ->
                mode to target // 将两个 Flow 的最新值合并成一个 Pair
            }.collect { (mode, target) ->
                _uiState.update { it.copy(isProcessing = true) }

                val currentState = _uiState.value
                val justSwitchedToBlur = mode == MOSAIC_MODE_BLUR && currentState.mosaicMode != MOSAIC_MODE_BLUR
                val targetChangedWhileInBlur = mode == MOSAIC_MODE_BLUR && target != currentState.mosaicTarget

                mosaicModeUpdateJob?.cancel()
                mosaicModeUpdateJob = viewModelScope.launch {
                    delay(200L) // 防抖

                    val currentState = _uiState.value
                    // 同时更新 mode 和 target
                    _uiState.update { it.copy(mosaicMode = mode, mosaicTarget = target) }

                    if (currentState.originalBitmap != null && currentState.detectionOutput != null) {
                        when (mode) {
                            MOSAIC_MODE_EMOJI -> {
                                _uiState.update { it.copy(blurRegions = emptyList()) }
                                calculateEmojiPositions(currentState.detectionOutput)
                            }
                            MOSAIC_MODE_BLUR -> {
                                _uiState.update {
                                    it.copy(
                                        selectedEmojis = emptyList(),
                                        editingEmoji = null
                                    )
                                }
                                if (justSwitchedToBlur || targetChangedWhileInBlur) {
                                    // 只有在满足上述条件时，才执行破坏性的重新计算
                                    calculateBlurRegions(currentState.detectionOutput)
                                } else {
                                    // 在其他情况下（例如，只是切换了模糊类型），我们保留现有编辑，
                                    // 只需结束“处理中”的状态即可。
                                    _uiState.update { it.copy(isProcessing = false) }
                                }
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isProcessing = false) }
                    }
                }
            }
        }
        viewModelScope.launch {
            preferenceRepository.mosaicType.collect { type ->
                _uiState.update { it.copy(mosaicType = type) }
            }
        }
    }

    fun setEasterEggEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateEasterEggState(enabled)
            // 如果关闭彩蛋，同时关闭 "Too Deep" 选项
            if (!enabled) {
                preferenceRepository.updateTooDeepState(false)
                // ✨ 新增：立刻清空UI状态中的 fakeDetections 列表
                _uiState.update { it.copy(fakeDetections = emptyList()) }
            }
        }
    }

    fun setTooDeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateTooDeepState(enabled)
            if (enabled) {
                _uiState.value.displayedBitmap?.let { bitmap ->
                    // 2. 如果有图，则立刻调用函数生成一个新的假识别框
                    val fakeBox = generateFakeDetection(bitmap.width, bitmap.height)
                    // 3. 更新UI状态，将新的假识别框列表应用到界面上
                    _uiState.update { it.copy(fakeDetections = listOf(fakeBox)) }
                }
            } else {
                _uiState.update { it.copy(fakeDetections = emptyList()) }
            }
        }
    }

    // --- Public Functions (Matching Original API as much as possible) ---

    /**
     * Clears the current image and processing results.
     * (Signature matches original)
     */
    fun clearImage() {
        _uiState.update {
            // Reset relevant parts of the state, keep settings
            it.copy(
                originalBitmap = null,
                displayedBitmap = null,
                selectedEmojis = emptyList(),
                isProcessing = false,
                isRendering = false,
                errorMessage = null,
                successMessage = null,
                editingEmoji = null,
                editingEmojiIndex = null,
                fakeDetections = emptyList(),
                detectionOutput = null, // ✨ 必须将之前的检测结果也清空
                blurRegions = emptyList(),
                aspectRatio = null
            )
        }
    }

    /**
     * Starts the face detection process for the given image Uri.
     * (Signature matches original)
     */
    fun detect(inputUri: Uri) {
        viewModelScope.launch { // UseCases handle their own Dispatchers
            clearImage() // Clear previous data first
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            getBitmapUseCase(inputUri).fold(
                onSuccess = { highResBitmap ->
                    // --- 核心修正 Start ---

                    // 1. 创建一个真正用于UI显示的、低分辨率的缩略图。
                    val displayBitmap = scaleBitmapIfNeeded(highResBitmap)

                    // 2. 计算并保存原图的宽高比，这对于UI布局至关重要。
                    val aspectRatio = if (highResBitmap.height > 0) highResBitmap.width.toFloat() / highResBitmap.height.toFloat() else 1f

                    // 3. 更新State：
                    //    - `originalBitmap` 保存高分辨率原图，以备后用。
                    //    - `displayedBitmap` 保存低分辨率预览图，用于UI和实时计算。
                    _uiState.update { it.copy(
                        originalBitmap = highResBitmap,
                        displayedBitmap = displayBitmap, // <--- 使用缩放后的图
                        aspectRatio = aspectRatio
                    )}

                    // 4. ✨ 将低分辨率的 `displayBitmap` 传递给 DetectFacesUseCase 进行处理。
                    //    后续所有的坐标计算都将自然地基于这张小图，从而保证坐标系的统一。
                    detectFacesUseCase(displayBitmap).fold(
                        // --- 核心修正 End ---
                        onSuccess = { detectionOutput ->
                            _uiState.update { it.copy(detectionOutput = detectionOutput) } // 保存结果
                            if (_uiState.value.isTooDeep) {
                                val sourceBitmap = detectionOutput.sourceBitmap
                                val fakeBox = generateFakeDetection(sourceBitmap.width, sourceBitmap.height)
                                _uiState.update { it.copy(fakeDetections = listOf(fakeBox)) }
                            } else {
                                // 如果选项是关闭的，确保列表是空的
                                _uiState.update { it.copy(fakeDetections = emptyList()) }
                            }
                            // 根据模式选择处理方式
                            when (_uiState.value.mosaicMode) {
                                MOSAIC_MODE_EMOJI -> calculateEmojiPositions(detectionOutput)
                                MOSAIC_MODE_BLUR -> calculateBlurRegions(detectionOutput)
                            }
                        },
                        onFailure = { exception ->
                            _uiState.update { it.copy(isProcessing = false, errorMessage = "Face detection failed: ${exception.localizedMessage}") }
                        }
                    )
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Face detection failed: ${exception.localizedMessage}") }
                }
            )
        }
    }

    // Private helper for detect flow
    private fun calculateEmojiPositions(detectionOutput: DetectionOutput) {
        viewModelScope.launch {
            calculateEmojiPositionsUseCase(detectionOutput).fold(
                onSuccess = { emojiDetections ->
                    _uiState.update {
                        it.copy(
                            selectedEmojis = emojiDetections,
                            isProcessing = false) } // Update emojis first
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Calculating positions failed: ${exception.localizedMessage}") }
                }
            )
        }
    }

    private fun calculateBlurRegions(detectionOutput: DetectionOutput) {
        viewModelScope.launch {
            val target = _uiState.value.mosaicTarget
            calculateBlurRegionsUseCase(detectionOutput, target).fold(
                onSuccess = { regions ->
                    regions.forEachIndexed { index, region ->
                        Log.d("MojiDebug", "[ViewModel] Generated BlurRegion[$index]: Rect=${region.rect.toShortString()}, Angle=${"%.1f".format(region.angle)}")
                    }
                    _uiState.update {
                        it.copy(
                            blurRegions = regions,
                            isProcessing = false, // 结束处理状态
                            displayedBitmap = it.detectionOutput?.sourceBitmap // 在模糊模式，直接显示源图（低分辨率）
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Calculating blur regions failed: ${exception.localizedMessage}") }
                }
            )
        }
    }

    /**
     * 关键修改：创建一个按需渲染最终 Bitmap 的挂起函数
     */
    private suspend fun renderFinalBitmap(): Bitmap? {
        val highResBitmap = _uiState.value.originalBitmap ?: return null
        val displayBitmap = _uiState.value.displayedBitmap ?: return null
        val mode = _uiState.value.mosaicMode

        // 计算高分辨率图相对于显示图的缩放比例
        val scale = if (displayBitmap.width > 0) highResBitmap.width.toFloat() / displayBitmap.width.toFloat() else 1.0f

        return when (mode) {
            MOSAIC_MODE_EMOJI -> {
                // 升格 (Upscale) Emoji 的坐标和大小
                val scaledEmojis = _uiState.value.selectedEmojis.map {
                    it.copy(
                        xCenter = it.xCenter * scale,
                        yCenter = it.yCenter * scale,
                        diameter = it.diameter * scale
                    )
                }
                val font = _uiState.value.selectedFontPath
                // 在高分辨率图上渲染
                renderEmojiOnBitmapUseCase(highResBitmap, scaledEmojis, font).getOrNull()
            }
            MOSAIC_MODE_BLUR -> {
                // 升格 (Upscale) 模糊区域的坐标和大小
                val scaledRegions = _uiState.value.blurRegions.map {
                    val newRect = RectF(
                        it.rect.left * scale,
                        it.rect.top * scale,
                        it.rect.right * scale,
                        it.rect.bottom * scale
                    )
                    it.copy(rect = newRect)
                }
                val type = _uiState.value.mosaicType
                val blurType = when (type) {
                    PreferenceRepository.MOSAIC_TYPE_PIXELATED -> BlurType.Pixelated
                    PreferenceRepository.MOSAIC_TYPE_HALFTONE -> BlurType.Halftone
                    else -> BlurType.Gaussian
                }
                // 在高分辨率图上渲染
                renderMosaicOnBitmapUseCase(highResBitmap, scaledRegions, blurType).getOrNull()
            }
            else -> highResBitmap // 默认返回原图
        }
    }

    /**
     * Shares the processed image.
     * (Signature matches original, but bitmap parameter is ignored)
     */
    fun shareImage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            try {
                val finalBitmap = renderFinalBitmap()
                if (finalBitmap == null) {
                    _shareEvent.emit(
                        ShareEvent.Error(
                            "Failed to render final image for sharing.",
                            Constants.STATUS_SHARE
                        )
                    )
                    return@launch
                }

                generateShareableUriUseCase(finalBitmap).fold(
                    onSuccess = { uri ->
                        _shareEvent.emit(ShareEvent.ShareImage(uri))
                    },
                    onFailure = { exception ->
                        exception.localizedMessage?.let {
                            ShareEvent.Error(
                                message = it,
                                status = Constants.STATUS_SHARE
                            )
                        }?.let { _shareEvent.emit(it) }
                    }
                )
            } finally {
                _uiState.update { it.copy(isRendering = false) }
            }
        }
    }

    /**
     * Saves the processed image to the gallery.
     * (Signature matches original, but bitmap parameter is ignored)
     */
    fun saveImageToGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            try {
                val finalBitmap = renderFinalBitmap()
                if (finalBitmap == null) {
                    _shareEvent.emit(
                        ShareEvent.Error(
                            "Failed to render final image for saving.",
                            Constants.STATUS_SAVE
                        )
                    )
                    return@launch
                }

                saveImageUseCase(finalBitmap).fold(
                    onSuccess = {
                        _shareEvent.emit(ShareEvent.Success(Constants.STATUS_SAVE))
                    },
                    onFailure = { exception ->
                        exception.localizedMessage?.let {
                            ShareEvent.Error(
                                message = it,
                                status = Constants.STATUS_SAVE
                            )
                        }
                            ?.let { _shareEvent.emit(it) }
                    }
                )
            } finally {
                _uiState.update { it.copy(isRendering = false) }
            }
        }
    }

    /**
     * Returns a random emoji from the current predefined list.
     * (Signature matches original)
     */
    fun getRandomEmoji(): String {
        return _uiState.value.predefinedEmojiOptions.randomOrNull() ?: "❓" // Default fallback
    }

    /**
     * 当用户在“添加模式”下点击图片时调用。
     * 此函数会根据当前的 mosaicMode 决定是添加 Emoji 还是 Blur Region。
     */
    fun addItemAtPosition(x: Float, y: Float) {
        val state = _uiState.value
        when (state.mosaicMode) {
            MOSAIC_MODE_EMOJI -> {
                // --- 复用原有的 Emoji 添加逻辑 ---
                addNewEmoji(state, x, y)
            }
            MOSAIC_MODE_BLUR -> {
                // --- 调用我们刚刚创建的新函数 ---
                addNewBlurRegion(x, y)
            }
        }
    }

    private fun addNewEmoji(
        state: EditUiState,
        x: Float,
        y: Float
    ) {
        if (state.displayedBitmap == null) {
            _uiState.update { it.copy(errorMessage = "Cannot add emoji without an image.") }
            return
        }
        val newDetection = EmojiDetection(
            xCenter = x,
            yCenter = y,
            diameter = 100f,
            angle = 0f,
            emoji = getRandomEmoji()
        )
        _uiState.update {
            it.copy(
                editingEmoji = newDetection,
                editingEmojiIndex = -1 // 使用 -1 标记为新增
            )
        }
    }

    fun onSlidingStateChanged(isSliding: Boolean) {
        _uiState.update { it.copy(isSliding = isSliding) }
        // 如果滑动结束，并且当前在编辑 blur 模式，则触发一次重绘
        if (!isSliding && _uiState.value.mosaicMode == MOSAIC_MODE_BLUR) {
            triggerReRender()
        }
    }

    private var renderJob: Job? = null
    private fun triggerReRender() {
        renderJob?.cancel() // 取消任何正在进行的渲染
        renderJob = viewModelScope.launch {
            _uiState.update { it.copy(isRendering = true) }
            val state = _uiState.value
            val baseBitmap = state.displayedBitmap ?: return@launch
            val regions = state.blurRegions
            rerenderBlurEffect(baseBitmap, regions, state.mosaicType)
        }
    }

    // --- Emoji Manipulation ---

    /**
     * Removes an emoji from the list by its index.
     */
    fun removeEmoji(index: Int) {
        val currentList = _uiState.value.selectedEmojis.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList.removeAt(index)
            _uiState.update { it.copy(selectedEmojis = currentList) }
        }
    }

    /**
     * 开始编辑一个 Emoji
     */
    fun startEditing(index: Int) {
        val emojiToEdit = _uiState.value.selectedEmojis.getOrNull(index)?.copy() ?: return
        _uiState.update {
            it.copy(
                editingEmojiIndex = index,
                editingEmoji = emojiToEdit
            )
        }
        editingStateFlow.value = emojiToEdit // 触发 flow
    }

    /**
     * 当滑块或输入框变化时，实时更新瞬时状态
     */
    fun updateEditingEmoji(emoji: String? = null, diameter: Float? = null, angle: Float? = null) {
        val currentEditing = _uiState.value.editingEmoji ?: return
        val updatedEmoji = currentEditing.copy(
            emoji = emoji ?: currentEditing.emoji,
            diameter = diameter ?: currentEditing.diameter,
            angle = angle ?: currentEditing.angle
        )
        _uiState.update { it.copy(editingEmoji = updatedEmoji) }
    }

    fun addNewBlurRegion(tapPositionX: Float, tapPositionY: Float) {
        if (_uiState.value.displayedBitmap == null) {
            _uiState.update { it.copy(errorMessage = "Cannot add blur region without an image.") }
            return
        }

        // 1. 创建一个新的、默认的 BlurRegion 对象
        //  - RectF 定义了位置和初始大小，可以设置为一个固定的初始尺寸。
        //  - angle 初始为 0。
        val initialSize = 200f // 举例：初始直径为 200 像素
        val newRegionRect = RectF(
            tapPositionX - initialSize / 2,
            tapPositionY - initialSize / 2,
            tapPositionX + initialSize / 2,
            tapPositionY + initialSize / 2
        )
        val newBlurRegion = BlurRegion(
            rect = newRegionRect,
            angle = 0f,
            originalRect = newRegionRect // 将初始矩形也记录下来
        )

        // 2. 更新 UI State，将这个新区域设置为正在编辑的区域
        //    - `editingBlurRegion` 用于在 Overlay 中实时预览。
        //    - `editingBlurRegionIndex` 设置为 -1 (或任何不在列表中的值)，
        //      用于告诉 `confirmEditing` 这是一个“新增”操作。
        _uiState.update {
            it.copy(
                editingBlurRegion = newBlurRegion,
                editingBlurRegionIndex = -1, // 使用 -1 标记为新增
                // 确保清空 Emoji 的编辑状态，避免冲突
                editingEmoji = null,
                editingEmojiIndex = null
            )
        }
    }

    // --- 新增：选中模糊区域进行编辑 ---
    fun selectBlurRegionForEditing(index: Int) {
        val region = _uiState.value.blurRegions.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                editingBlurRegionIndex = index,
                editingBlurRegion = region,
                // 同时清空 Emoji 的编辑状态，确保互斥
                editingEmojiIndex = null,
                editingEmoji = null
            )
        }
    }

    fun setMosaicTarget(target: Int) {
        viewModelScope.launch {
            preferenceRepository.setMosaicTarget(target)
        }
    }

    /**
     * 更新正在编辑的 Emoji 的大小。
     * @param factor 新的大小比例因子 (例如，1.2f 表示放大20%)。
     */
    fun updateEditingEmojiSize(factor: Float) {
        val currentEditingEmoji = _uiState.value.editingEmoji ?: return

        // 获取原始 Emoji 以计算基准大小
        val originalDiameter = currentEditingEmoji.originalDiameter

        // 计算新的直径
        val newDiameter = originalDiameter * factor
        // 更新瞬时编辑状态
        _uiState.update {
            it.copy(editingEmoji = currentEditingEmoji.copy(diameter = newDiameter))
        }
    }

    /**
     * 更新正在编辑的 Blur 区域的大小。
     * @param factor 新的大小比例因子。
     */
    fun updateEditingBlurRegionSize(factor: Float) {
        val currentEditingRegion = _uiState.value.editingBlurRegion ?: return

        // 获取原始 Blur 区域以计算基准大小
        val originalRect = currentEditingRegion.originalRect

        val originalWidth = originalRect.width()
        val originalHeight = originalRect.height()
        val centerX = originalRect.centerX()
        val centerY = originalRect.centerY()

        // 计算新的宽高
        val newWidth = originalWidth * factor
        val newHeight = originalHeight * factor

        // 创建新的 RectF
        val newRect = RectF(
            centerX - newWidth / 2,
            centerY - newHeight / 2,
            centerX + newWidth / 2,
            centerY + newHeight / 2
        )

        // 更新瞬时编辑状态
        _uiState.update {
            it.copy(editingBlurRegion = currentEditingRegion.copy(rect = newRect))
        }
    }

    /**
     * 更新正在编辑的对象的角度 (通用)。
     * @param newAngle 新的角度。
     */
    fun updateEditingAngle(newAngle: Float) {
        // 根据模式更新对应的瞬时状态
        if (_uiState.value.mosaicMode == MOSAIC_MODE_EMOJI) {
            _uiState.value.editingEmoji?.let { emoji ->
                _uiState.update { it.copy(editingEmoji = emoji.copy(angle = newAngle)) }
            }
        } else { // MOSAIC_MODE_BLUR
            _uiState.value.editingBlurRegion?.let { region ->
                _uiState.update { it.copy(editingBlurRegion = region.copy(angle = newAngle)) }
            }
        }
    }

    /**
     * 用户点击"确定"，确认修改。
     * 此函数现在可以同时处理 Emoji 和 Blur 模式的确认操作。
     */
    fun confirmEditing() {
        val state = _uiState.value

        when (state.mosaicMode) {
            MOSAIC_MODE_EMOJI -> {
                val transientEmoji = state.editingEmoji ?: return
                val transientIndex = state.editingEmojiIndex
                val currentList = state.selectedEmojis.toMutableList()

                if (transientEmoji.emoji.isEmpty()) {
                    // 如果是删除操作 (清空文本)
                    if (transientIndex != null && transientIndex >= 0 && transientIndex < currentList.size) {
                        currentList.removeAt(transientIndex)
                    }
                } else {
                    // 新增或更新操作
                    if (transientIndex != null && transientIndex >= 0 && transientIndex < currentList.size) {
                        currentList[transientIndex] = transientEmoji // 更新
                    } else {
                        currentList.add(transientEmoji) // 新增
                    }
                }
                _uiState.update { it.copy(selectedEmojis = currentList) }
            }
            MOSAIC_MODE_BLUR -> {
                val editedRegion = state.editingBlurRegion ?: return
                val index = state.editingBlurRegionIndex ?: return
                val currentList = state.blurRegions.toMutableList()

                if (index >= 0 && index < currentList.size) {
                    // 更新现有区域
                    currentList[index] = editedRegion
                } else {
                    // 如果 index 是 -1 或 null，则为新增
                    currentList.add(editedRegion)
                }
                _uiState.update { it.copy(blurRegions = currentList) }
            }
        }

        // 统一在最后退出编辑状态，清除所有编辑相关的瞬时数据
        cancelEditing()
    }

    /**
     * 用户取消编辑。
     * 此函数现在会清除所有模式下的编辑状态，使其成为一个通用的取消操作。
     */
    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingEmoji = null,
                editingEmojiIndex = null,
                editingBlurRegion = null,
                editingBlurRegionIndex = null
            )
        }
    }

    // --- Settings Related ---

    /**
     * Updates the list of predefined emojis used for random selection.
     * (Signature matches original)
     */
    fun updateEmojiList(emojis: String) {
        viewModelScope.launch {
            // Using a dedicated Use Case is cleaner
            updateEmojiOptionsUseCase(emojis).fold(
                onSuccess = { /* Preferences flow will update state */ },
                onFailure = { exception -> _uiState.update { it.copy(errorMessage = "Failed to update emoji list: ${exception.localizedMessage}") } }
            )
        }
    }

    /**
     * Toggles the visibility of the app's launcher icon.
     * (Signature matches original)
     */
    fun toggleLauncherIcon(hideIcon: Boolean) {
        viewModelScope.launch {
            manageAppIconVisibilityUseCase(hideIcon).fold(
                onSuccess = { /* Preferences flow will update state */ },
                onFailure = { exception -> _uiState.update { it.copy(errorMessage = "Failed to toggle icon visibility: ${exception.localizedMessage}") } }
            )
        }
    }

    /**
     * Copies a font file from the given Uri to internal storage.
     * (Signature matches original)
     */
    fun copyFontToInternal(uri: Uri) {
        viewModelScope.launch {
            manageFontUseCase.addFont(uri).fold(
                onSuccess = { /* Preferences flow will update state */ },
                onFailure = { exception -> _uiState.update { it.copy(errorMessage = "Failed to add font: ${exception.localizedMessage}") } }
            )
        }
    }

    /**
     * Removes a font file from internal storage by its path.
     * (Signature matches original)
     */
    fun removeFontFromInternal(filePath: String) {
        viewModelScope.launch {
            manageFontUseCase.removeFont(filePath).fold(
                onSuccess = {
                    // Preferences flow will update state (list and potentially selected font)
                    // Font change might require rerender, handled by flow observer now
                },
                onFailure = { exception -> _uiState.update { it.copy(errorMessage = "Failed to remove font: ${exception.localizedMessage}") } }
            )
        }
    }

    /**
     * Called when the user selects a font from the dropdown.
     * (Signature matches original)
     */
    fun onFontSelected(selectedIndex: Int) {
        val selectedPath = _uiState.value.availableFontPaths.getOrNull(selectedIndex) ?: Constants.DEFAULT_FONT_MARKER
        if (selectedPath == _uiState.value.selectedFontPath) return // No change

        viewModelScope.launch {
            manageFontUseCase.selectFont(selectedPath).fold(
                onSuccess = {
                    // Preferences flow will update state (selected path and loaded font)
                    // Rerender is handled by the flow observer if needed
                },
                onFailure = { exception -> _uiState.update { it.copy(errorMessage = "Failed to select font: ${exception.localizedMessage}") } }
            )
        }
    }

    fun setMosaicMode(mode: Int) {
        viewModelScope.launch {
            preferenceRepository.setMosaicMode(mode)
        }
    }

    fun setMosaicType(type: Int) {
        viewModelScope.launch {
            preferenceRepository.setMosaicType(type)
        }
    }

    /** Clears the current error message from the state */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Clears the current success message from the state */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun rerenderBlurEffect(baseBitmap: Bitmap, regions: List<BlurRegion>, type: Int) {
        viewModelScope.launch {
            // 将 Int 类型转换为我们定义的 BlurType
            val blurType = when (type) {
                PreferenceRepository.MOSAIC_TYPE_PIXELATED -> BlurType.Pixelated
                PreferenceRepository.MOSAIC_TYPE_HALFTONE -> BlurType.Halftone
                else -> BlurType.Gaussian
            }

            // 调用 UseCase 进行渲染
            renderMosaicOnBitmapUseCase(baseBitmap, regions, blurType).fold(
                onSuccess = { renderedBitmap ->
                    // 更新用于显示的 processedBitmap
                    _uiState.update {
                        it.copy(
                            displayedBitmap = renderedBitmap,
                            isRendering = false
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Re-rendering failed: ${exception.localizedMessage}",
                            isRendering = false
                        )
                    }
                }
            )
        }
    }

    private fun generateFakeDetection(bitmapWidth: Int, bitmapHeight: Int): FakeDetection {
        // --- START: 修改的代码 ---
        // 1. 确定一个基准尺寸，防止矩形过大或过小
        //    取图片较短边的 1/6 到 1/4 之间作为基准
        val baseDimension = Random.nextInt(
            from = minOf(bitmapWidth, bitmapHeight) / 6,
            until = minOf(bitmapWidth, bitmapHeight) / 4
        )

        // 2. 让宽度和高度在基准尺寸上下小范围浮动，确保它们的值很接近
        //    这里的浮动范围是基准尺寸的 +/- 10%
        val variance = (baseDimension * 0.1f).toInt()
        val width = (baseDimension + Random.nextInt(-variance, variance + 1)).toFloat()
        val height = (baseDimension + Random.nextInt(-variance, variance + 1)).toFloat()

        // 3. 确保随机生成的位置不会让框超出图片边界
        val left = Random.nextInt(0, bitmapWidth - width.toInt()).toFloat()
        val top = Random.nextInt(0, bitmapHeight - height.toInt()).toFloat()
        // --- END: 修改的代码 ---

        val startAge = Random.nextInt(300, 990)
        val endAge = startAge + Random.nextInt(4, 21)

        return FakeDetection(
            box = RectF(left, top, left + width, top + height),
            label = "face",
            confidence = Random.nextDouble(0.95, 0.99).toFloat(),
            startAge = startAge,
            endAge = endAge
        )
    }
}