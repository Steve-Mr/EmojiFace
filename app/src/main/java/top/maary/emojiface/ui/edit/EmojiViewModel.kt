package top.maary.emojiface.ui.edit // Or your chosen package

// Imports for Android, Lifecycle, Coroutines, Hilt, Flows, Graphics etc.
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
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
import javax.inject.Inject
import kotlin.random.Random

// Define the UI State Data Class (can be in a separate file)
data class EditUiState(
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
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
    val isEasterEggEnabled: Boolean = false,
    val isTooDeep: Boolean = false,
    val fakeDetections: List<FakeDetection> = emptyList(), // 存储假识别框
    val detectionOutput: DetectionOutput? = null, // 保存原始检测结果
    val mosaicMode: Int = MOSAIC_MODE_EMOJI, // 新增马赛克模式状态
    val blurRegions: List<BlurRegion> = emptyList()
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
            preferenceRepository.mosaicMode.collect { newMode ->
                _uiState.update { it.copy(isProcessing = true) }
                // --- START: 防抖逻辑 ---

                // 2. 取消上一个还未执行的更新任务
                mosaicModeUpdateJob?.cancel()

                // 3. 启动一个新的协程，并持有它的 Job
                mosaicModeUpdateJob = viewModelScope.launch {
                    // 4. 等待 200 毫秒。如果在此期间有新的模式切换，这个协程会被取消
                    delay(200L)

                    // 只有在用户停止切换 200 毫秒后，才会执行以下逻辑
                    val currentState = _uiState.value
                    _uiState.update { it.copy(mosaicMode = newMode) }

                    if (currentState.originalBitmap != null && currentState.detectionOutput != null) {
                        when (newMode) {
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
                                calculateBlurRegions(currentState.detectionOutput)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setEasterEggEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateEasterEggState(enabled)
            // 如果关闭彩蛋，同时关闭 "Too Deep" 选项
            if (!enabled) {
                preferenceRepository.updateTooDeepState(false)
            }
        }
    }

    fun setTooDeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateTooDeepState(enabled)
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
                processedBitmap = null, // 同时清除 processedBitmap
                selectedEmojis = emptyList(),
                isProcessing = false,
                isRendering = false,
                errorMessage = null,
                successMessage = null,
                editingEmoji = null,
                editingEmojiIndex = null,
                // --- START: 新增代码 ---
                fakeDetections = emptyList()
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
                onSuccess = { bitmap ->
                    _uiState.update { it.copy( originalBitmap = bitmap)}
                    detectFacesUseCase(bitmap).fold(
                        onSuccess = { detectionOutput ->
                            _uiState.update { it.copy(detectionOutput = detectionOutput) } // 保存结果
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
                    _uiState.update { it.copy(selectedEmojis = emojiDetections) } // Update emojis first
                    renderInitialBitmap(detectionOutput.originalBitmap, emojiDetections) // Proceed to render
                    if (_uiState.value.isTooDeep) {
                        val originalBitmap = detectionOutput.originalBitmap
                        val fakeBox = generateFakeDetection(originalBitmap.width, originalBitmap.height)
                        _uiState.update { it.copy(fakeDetections = listOf(fakeBox)) }
                    } else {
                        _uiState.update { it.copy(fakeDetections = emptyList()) }
                    }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Calculating positions failed: ${exception.localizedMessage}") }
                }
            )
        }
    }

    private fun calculateBlurRegions(detectionOutput: DetectionOutput) {
        viewModelScope.launch {
            calculateBlurRegionsUseCase(detectionOutput).fold(
                onSuccess = { regions ->
                    _uiState.update {
                        it.copy(
                            blurRegions = regions,
                            isProcessing = false // 结束处理状态
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
        val base = _uiState.value.originalBitmap ?: return null
        val mode = _uiState.value.mosaicMode

        return when (mode) {
            MOSAIC_MODE_EMOJI -> {
                val emojis = _uiState.value.selectedEmojis
                val font = _uiState.value.selectedFontPath
                renderEmojiOnBitmapUseCase(base, emojis, font).getOrNull()
            }
            MOSAIC_MODE_BLUR -> {
                val regions = _uiState.value.blurRegions
                renderMosaicOnBitmapUseCase(base, regions, BlurType.Gaussian).getOrNull()
            }
            else -> base // 默认返回原图
        }
    }

    // Private helper for detect flow
    private fun renderInitialBitmap(baseBitmap: Bitmap, emojiDetections: List<EmojiDetection>) {
        viewModelScope.launch {
            val selectedFont = _uiState.value.selectedFontPath
            renderEmojiOnBitmapUseCase(baseBitmap, emojiDetections, selectedFont).fold(
                onSuccess = { renderedBitmap ->
                    _uiState.update {
                        it.copy(
                            // originalBitmap is already set
                            processedBitmap = renderedBitmap,
                            isProcessing = false // Entire initial process complete
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Rendering failed: ${exception.localizedMessage}") }
                }
            )
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

    // --- Emoji Manipulation ---

    /**
     * Adds a new emoji at the specified coordinates.
     * (Signature matches original)
     */
    fun addEmoji(x: Float, y: Float, emoji: String, diameter: Float, angle: Float) {
        if (_uiState.value.originalBitmap == null) {
            _uiState.update { it.copy(errorMessage = "Cannot add emoji without an image.") }
            return
        }
        // 1. 创建新的 Emoji 检测对象
        val newDetection = EmojiDetection(xCenter = x, yCenter = y, diameter = diameter, angle = angle, emoji = emoji)

        // 2. 不将其添加到主列表，而是直接放入瞬时编辑状态
        _uiState.update {
            it.copy(
                editingEmoji = newDetection,
                editingEmojiIndex = -1 // 使用 -1 来标记这是一个“添加”操作，而非“编辑”
            )
        }
    }

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

    /**
     * 用户点击"确定"，确认修改
     */
    fun confirmEditing() {
        val transientEmoji = _uiState.value.editingEmoji ?: return
        val transientIndex = _uiState.value.editingEmojiIndex
        val currentList = _uiState.value.selectedEmojis.toMutableList()

        if (transientEmoji.emoji.isEmpty()) {
            // This is a delete operation. Only delete if it's an existing emoji.
            if (transientIndex != null && transientIndex >= 0 && transientIndex < currentList.size) {
                currentList.removeAt(transientIndex)
            }
            // If it was a new emoji, clearing text is just a cancel, so we do nothing.
        } else {
            // This is an add or update operation.
            if (transientIndex != null && transientIndex >= 0 && transientIndex < currentList.size) {
                // Update existing emoji
                currentList[transientIndex] = transientEmoji
            } else if (transientIndex != null && transientIndex == -1) {
                // Add new emoji
                currentList.add(transientEmoji)
            }
        }

        // 清除瞬时编辑状态
        _uiState.update { it.copy(selectedEmojis = currentList, editingEmoji = null, editingEmojiIndex = null) }
        editingStateFlow.value = null
    }

    /**
     * 用户取消编辑
     */
    fun cancelEditing() {
        _uiState.update { it.copy(editingEmoji = null, editingEmojiIndex = null) }
        editingStateFlow.value = null
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

    /** Clears the current error message from the state */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Clears the current success message from the state */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
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