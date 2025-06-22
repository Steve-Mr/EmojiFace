package top.maary.emojiface.ui.edit // Or your chosen package

// Imports for Android, Lifecycle, Coroutines, Hilt, Flows, Graphics etc.
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import top.maary.emojiface.domain.usecase.CalculateEmojiPositionsUseCase
import top.maary.emojiface.domain.usecase.DetectFacesUseCase
import top.maary.emojiface.domain.usecase.DetectionOutput
import top.maary.emojiface.domain.usecase.GenerateShareableUriUseCase
import top.maary.emojiface.domain.usecase.GetBitmapUseCase
import top.maary.emojiface.domain.usecase.ManageAppIconVisibilityUseCase
import top.maary.emojiface.domain.usecase.ManageFontUseCase
import top.maary.emojiface.domain.usecase.RenderEmojiOnBitmapUseCase
import top.maary.emojiface.domain.usecase.SaveImageUseCase
import top.maary.emojiface.domain.usecase.UpdateEmojiOptionsUseCase
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getTypeFaceFromPath
import top.maary.emojiface.util.loadFontFromPath
import javax.inject.Inject

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
)

@HiltViewModel
class EmojiViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository, // Keep for observing flows
    // Inject Use Cases
    private val detectFacesUseCase: DetectFacesUseCase,
    private val calculateEmojiPositionsUseCase: CalculateEmojiPositionsUseCase,
    private val renderEmojiOnBitmapUseCase: RenderEmojiOnBitmapUseCase,
    private val manageFontUseCase: ManageFontUseCase, // Handles add/remove/select
    private val saveImageUseCase: SaveImageUseCase,
    private val generateShareableUriUseCase: GenerateShareableUriUseCase,
    private val manageAppIconVisibilityUseCase: ManageAppIconVisibilityUseCase, // Assuming created
    private val updateEmojiOptionsUseCase: UpdateEmojiOptionsUseCase, // Assuming created
    private val getBitmapUseCase: GetBitmapUseCase
    ) : ViewModel() {

    // --- State Management ---
    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    // --- Event Channel ---
    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent.asSharedFlow()

    private val editingStateFlow = MutableStateFlow<EmojiDetection?>(null)

    init {
        // Observe preferences and update state
        observePreferences()
//        viewModelScope.launch {
//            editingStateFlow
//                .debounce(50L) // 防抖，用户停止滑动50毫秒后再触发
//                .collect { emoji ->
//                    if (emoji != null) {
//                        // 当有稳定的编辑中状态时，触发重绘
//                        rerenderWithTransientEdit()
//                    }
//                }
//        }
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
                editingEmojiIndex = null
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
                            calculateEmojiPositions(detectionOutput) // Proceed to next step
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
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = "Calculating positions failed: ${exception.localizedMessage}") }
                }
            )
        }
    }

    /**
     * 关键修改：创建一个按需渲染最终 Bitmap 的挂起函数
     */
    private suspend fun renderFinalBitmap(): Bitmap? {
        val base = _uiState.value.originalBitmap ?: return null
        val emojis = _uiState.value.selectedEmojis
        val font = _uiState.value.selectedFontPath

        val result = renderEmojiOnBitmapUseCase(base, emojis, font)

        return result.getOrNull()
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

        if (transientIndex != null && transientIndex == -1) {
            // 情况2：这是一个新的 Emoji，现在将它添加到主列表中
            currentList.add(transientEmoji)
        } else if (transientIndex != null && transientIndex >= 0 && transientIndex < currentList.size) {
            // 情况1：这是一个已存在的 Emoji，更新它
            currentList[transientIndex] = transientEmoji
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

    /** Clears the current error message from the state */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Clears the current success message from the state */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}