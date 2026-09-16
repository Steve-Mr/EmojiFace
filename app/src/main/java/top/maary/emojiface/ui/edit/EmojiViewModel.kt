package top.maary.emojiface.ui.edit

import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.geometry.Point2D
import top.maary.emojiface.domain.model.geometry.Size2D
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.domain.repository.AvatarRepository
import top.maary.emojiface.domain.usecase.DetectFaceGeometriesUseCase
import top.maary.emojiface.domain.usecase.GenerateShareableUriUseCase
import top.maary.emojiface.domain.usecase.GetBitmapUseCase
import top.maary.emojiface.domain.usecase.ManageAppIconVisibilityUseCase
import top.maary.emojiface.domain.usecase.ManageFontUseCase
import top.maary.emojiface.domain.usecase.RenderOcclusionsOnBitmapUseCase
import top.maary.emojiface.domain.usecase.SaveImageUseCase
import top.maary.emojiface.domain.usecase.UpdateEmojiOptionsUseCase
import top.maary.emojiface.ui.edit.model.FakeDetection
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getTypeFaceFromPath
import top.maary.emojiface.util.loadFontFromPath
import top.maary.emojiface.util.scaleBitmapIfNeeded
import javax.inject.Inject
import kotlin.random.Random

data class EditUiState(
    val originalUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val displayedBitmap: Bitmap? = null,
    val occlusions: List<FaceOcclusion> = emptyList(),
    val editingOcclusionIndex: Int? = null,
    val editingOcclusion: FaceOcclusion? = null,
    val availableAvatars: List<Avatar> = emptyList(),
    val cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    val defaultMosaicMode: Int = PreferenceRepository.MOSAIC_MODE_EMOJI,
    val selectedDefaultAvatarId: String = PreferenceRepository.DEFAULT_AVATAR_ID,
    val predefinedEmojiOptions: List<String> = PreferenceRepository.DEFAULT_EMOJI_LIST,
    val isAppIconHidden: Boolean = false,
    val availableFontPaths: List<String> = listOf(Constants.DEFAULT_FONT_MARKER),
    val selectedFontPath: String = Constants.DEFAULT_FONT_MARKER,
    val loadedFontFamily: FontFamily? = null,
    val loadedTypeface: Typeface? = null,
    val isProcessing: Boolean = false,
    val isRendering: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isEasterEggEnabled: Boolean = false,
    val isTooDeep: Boolean = false,
    val fakeDetections: List<FakeDetection> = emptyList(),
    val mosaicMode: Int = MOSAIC_MODE_EMOJI,
    val mosaicType: Int = PreferenceRepository.MOSAIC_TYPE_GAUSSIAN,
    val mosaicTarget: Int = PreferenceRepository.MOSAIC_TARGET_FACE,
    val isSliding: Boolean = false,
    val isEditMode: Boolean = false,
    val shakeEditButtonTrigger: Long = 0L,
    val aspectRatio: Float? = null
)

@HiltViewModel
class EmojiViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
    private val avatarRepository: AvatarRepository,
    private val detectFaceGeometriesUseCase: DetectFaceGeometriesUseCase,
    private val renderOcclusionsOnBitmapUseCase: RenderOcclusionsOnBitmapUseCase,
    private val manageFontUseCase: ManageFontUseCase,
    private val saveImageUseCase: SaveImageUseCase,
    private val generateShareableUriUseCase: GenerateShareableUriUseCase,
    private val manageAppIconVisibilityUseCase: ManageAppIconVisibilityUseCase,
    private val updateEmojiOptionsUseCase: UpdateEmojiOptionsUseCase,
    private val getBitmapUseCase: GetBitmapUseCase
) : ViewModel() {

    // --- State Management ---
    private val _uiState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    // --- Event Channel ---
    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent.asSharedFlow()

    init {
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
                val fontFamily = loadFontFromPath(selectedPath)
                val typeface = if (selectedPath == Constants.DEFAULT_FONT_MARKER) {
                    Typeface.DEFAULT
                } else {
                    getTypeFaceFromPath(selectedPath)
                }
                Triple(paths, selectedPath, Pair(fontFamily, typeface))
            }.collect { (paths, selectedPath, fontPair) ->
                val (fontFamily, typeface) = fontPair
                _uiState.update { currentState ->
                    currentState.copy(
                        availableFontPaths = paths,
                        selectedFontPath = selectedPath,
                        loadedFontFamily = fontFamily,
                        loadedTypeface = typeface
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
            preferenceRepository.mosaicMode.collect { mode ->
                _uiState.update { state ->
                    val updatedOcclusions = state.occlusions.map { occ ->
                        if (!occ.isCustomized) {
                            val newMode = when (mode) {
                                PreferenceRepository.MOSAIC_MODE_BLUR -> OcclusionMode.Blur(
                                    type = mapMosaicType(state.mosaicType),
                                    target = mapMosaicTarget(state.mosaicTarget)
                                )
                                else -> OcclusionMode.Emoji(
                                    emoji = getRandomEmoji(),
                                    fontPath = state.selectedFontPath
                                )
                            }
                            occ.withMode(newMode)
                        } else {
                            occ
                        }
                    }
                    state.copy(
                        defaultMosaicMode = mode,
                        mosaicMode = mode,
                        occlusions = updatedOcclusions
                    )
                }
            }
        }
        viewModelScope.launch {
            preferenceRepository.mosaicType.collect { type ->
                _uiState.update { it.copy(mosaicType = type) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.mosaicTarget.collect { target ->
                _uiState.update { it.copy(mosaicTarget = target) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.selectedDefaultAvatarIdFlow.collect { avatarId ->
                _uiState.update { it.copy(selectedDefaultAvatarId = avatarId) }
            }
        }
        viewModelScope.launch {
            avatarRepository.avatarsFlow.collect { avatars ->
                val bitmapMap = mutableMapOf<String, Bitmap>()
                avatars.forEach { avatar ->
                    avatarRepository.loadAvatarBitmap(avatar.id).getOrNull()?.let { bmp ->
                        bitmapMap[avatar.id] = bmp
                    }
                }
                _uiState.update {
                    it.copy(
                        availableAvatars = avatars,
                        cachedAvatarBitmaps = bitmapMap
                    )
                }
            }
        }
    }

    private fun mapMosaicType(type: Int): OcclusionMode.Blur.BlurType = when (type) {
        PreferenceRepository.MOSAIC_TYPE_PIXELATED -> OcclusionMode.Blur.BlurType.Pixelated
        PreferenceRepository.MOSAIC_TYPE_HALFTONE -> OcclusionMode.Blur.BlurType.Halftone
        else -> OcclusionMode.Blur.BlurType.Gaussian
    }

    private fun mapMosaicTarget(target: Int): OcclusionMode.Blur.BlurTarget = when (target) {
        PreferenceRepository.MOSAIC_TARGET_EYES -> OcclusionMode.Blur.BlurTarget.Eyes
        else -> OcclusionMode.Blur.BlurTarget.Face
    }


    private fun createDefaultOcclusionMode(): OcclusionMode {
        val state = _uiState.value
        return when (state.defaultMosaicMode) {
            MOSAIC_MODE_BLUR -> OcclusionMode.Blur(
                type = mapMosaicType(state.mosaicType),
                target = mapMosaicTarget(state.mosaicTarget)
            )
            else -> OcclusionMode.Emoji(
                emoji = getRandomEmoji(),
                fontPath = state.selectedFontPath
            )
        }
    }

    fun setEasterEggEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateEasterEggState(enabled)
            if (!enabled) {
                preferenceRepository.updateTooDeepState(false)
                _uiState.update { it.copy(fakeDetections = emptyList()) }
            }
        }
    }

    fun setTooDeepEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.updateTooDeepState(enabled)
            if (enabled) {
                _uiState.value.displayedBitmap?.let { bitmap ->
                    val fakeBox = generateFakeDetection(bitmap.width, bitmap.height)
                    _uiState.update { it.copy(fakeDetections = listOf(fakeBox)) }
                }
            } else {
                _uiState.update { it.copy(fakeDetections = emptyList()) }
            }
        }
    }

    fun clearImage() {
        _uiState.update {
            it.copy(
                originalBitmap = null,
                displayedBitmap = null,
                occlusions = emptyList(),
                editingOcclusion = null,
                editingOcclusionIndex = null,
                isEditMode = false,
                shakeEditButtonTrigger = 0L,
                isProcessing = false,
                isRendering = false,
                errorMessage = null,
                successMessage = null,
                fakeDetections = emptyList(),
                aspectRatio = null
            )
        }
    }

    fun detect(inputUri: Uri) {
        viewModelScope.launch {
            clearImage()
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            getBitmapUseCase(inputUri).fold(
                onSuccess = { highResBitmap ->
                    val displayBitmap = scaleBitmapIfNeeded(highResBitmap)
                    val aspectRatio = if (highResBitmap.height > 0) {
                        highResBitmap.width.toFloat() / highResBitmap.height.toFloat()
                    } else 1f

                    _uiState.update {
                        it.copy(
                            originalUri = inputUri,
                            originalBitmap = highResBitmap,
                            displayedBitmap = displayBitmap,
                            aspectRatio = aspectRatio
                        )
                    }

                    detectFaceGeometriesUseCase(displayBitmap).fold(
                        onSuccess = { detectionResult ->
                            val defaultMode = _uiState.value.defaultMosaicMode

                            val occlusions = detectionResult.geometries.map { geometry ->
                                val mode: OcclusionMode = when (defaultMode) {
                                    MOSAIC_MODE_BLUR -> OcclusionMode.Blur(
                                        type = mapMosaicType(_uiState.value.mosaicType),
                                        target = mapMosaicTarget(_uiState.value.mosaicTarget)
                                    )
                                    else -> OcclusionMode.Emoji(
                                        emoji = getRandomEmoji(),
                                        fontPath = _uiState.value.selectedFontPath
                                    )
                                }
                                FaceOcclusion(geometry = geometry, mode = mode, isCustomized = false)
                            }

                            if (_uiState.value.isTooDeep) {
                                val fakeBox = generateFakeDetection(displayBitmap.width, displayBitmap.height)
                                _uiState.update { it.copy(fakeDetections = listOf(fakeBox)) }
                            } else {
                                _uiState.update { it.copy(fakeDetections = emptyList()) }
                            }

                            _uiState.update {
                                it.copy(
                                    occlusions = occlusions,
                                    isProcessing = false
                                )
                            }
                        },
                        onFailure = { exception ->
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    errorMessage = "Face detection failed: ${exception.localizedMessage}"
                                )
                            }
                        }
                    )
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Face detection failed: ${exception.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    private suspend fun renderFinalBitmap(): Bitmap? {
        val highResBitmap = _uiState.value.originalBitmap ?: return null
        val displayBitmap = _uiState.value.displayedBitmap ?: return null
        val occlusions = _uiState.value.occlusions

        val scale = if (displayBitmap.width > 0) {
            highResBitmap.width.toFloat() / displayBitmap.width.toFloat()
        } else 1.0f

        val scaledOcclusions = occlusions.map { occlusion ->
            occlusion.copy(geometry = occlusion.geometry.transform(scale))
        }

        val typefaces = _uiState.value.loadedTypeface?.let {
            mapOf(_uiState.value.selectedFontPath to it)
        } ?: emptyMap()

        return renderOcclusionsOnBitmapUseCase(
            baseBitmap = highResBitmap,
            occlusions = scaledOcclusions,
            loadedTypefaces = typefaces,
            cachedAvatarBitmaps = _uiState.value.cachedAvatarBitmaps,
            defaultTypeface = _uiState.value.loadedTypeface ?: Typeface.DEFAULT
        ).getOrNull()
    }

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

                val originalUri = _uiState.value.originalUri
                generateShareableUriUseCase(finalBitmap, originalUri).fold(
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

                val originalUri = _uiState.value.originalUri
                saveImageUseCase(finalBitmap, originalUri).fold(
                    onSuccess = {
                        _shareEvent.emit(ShareEvent.Success(Constants.STATUS_SAVE))
                    },
                    onFailure = { exception ->
                        exception.localizedMessage?.let {
                            ShareEvent.Error(
                                message = it,
                                status = Constants.STATUS_SAVE
                            )
                        }?.let { _shareEvent.emit(it) }
                    }
                )
            } finally {
                _uiState.update { it.copy(isRendering = false) }
            }
        }
    }

    fun getRandomEmoji(): String {
        return _uiState.value.predefinedEmojiOptions.randomOrNull() ?: "😂"
    }

    fun addItemAtPosition(x: Float, y: Float) {
        val state = _uiState.value
        if (state.displayedBitmap == null) {
            _uiState.update { it.copy(errorMessage = "Cannot add occlusion without an image.") }
            return
        }

        val mode = createDefaultOcclusionMode()
        val initialSize = 120f
        val geometry = FaceGeometry(
            center = Point2D(x, y),
            baseSize = Size2D(initialSize, initialSize),
            scaleFactor = 1.0f,
            rotationDegrees = 0f
        )

        val newOcclusion = FaceOcclusion(
            geometry = geometry,
            mode = mode,
            isCustomized = true
        )

        _uiState.update {
            it.copy(
                editingOcclusion = newOcclusion,
                editingOcclusionIndex = -1
            )
        }
    }

    fun onSlidingStateChanged(isSliding: Boolean) {
        _uiState.update { it.copy(isSliding = isSliding) }
    }

    // --- Occlusion Editing Operations ---

    fun startEditingOcclusion(index: Int) {
        val occlusion = _uiState.value.occlusions.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                editingOcclusionIndex = index,
                editingOcclusion = occlusion
            )
        }
    }

    fun updateEditingGeometry(scaleFactor: Float? = null, angle: Float? = null) {
        val current = _uiState.value.editingOcclusion ?: return
        val newGeometry = current.geometry.copy(
            scaleFactor = scaleFactor ?: current.geometry.scaleFactor,
            rotationDegrees = angle ?: current.geometry.rotationDegrees
        )
        _uiState.update {
            it.copy(editingOcclusion = current.copy(geometry = newGeometry))
        }
    }

    fun updateEditingMode(newMode: OcclusionMode) {
        val current = _uiState.value.editingOcclusion ?: return
        _uiState.update {
            it.copy(editingOcclusion = current.withMode(newMode))
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun triggerEditButtonShake() {
        _uiState.update { it.copy(shakeEditButtonTrigger = System.currentTimeMillis()) }
    }

    fun confirmEditing() {
        val state = _uiState.value
        val editing = state.editingOcclusion?.copy(isCustomized = true) ?: return
        val index = state.editingOcclusionIndex
        val currentList = state.occlusions.toMutableList()

        if (editing.mode is OcclusionMode.Emoji && editing.mode.emoji.isEmpty()) {
            if (index != null && index in currentList.indices) {
                currentList.removeAt(index)
            }
        } else {
            if (index != null && index in currentList.indices) {
                currentList[index] = editing
            } else {
                currentList.add(editing)
            }
        }

        _uiState.update {
            it.copy(
                occlusions = currentList,
                editingOcclusion = null,
                editingOcclusionIndex = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingOcclusion = null,
                editingOcclusionIndex = null
            )
        }
    }

    fun removeOcclusion(index: Int) {
        val currentList = _uiState.value.occlusions.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.update { it.copy(occlusions = currentList) }
        }
    }

    // --- Avatar Management Operations ---

    fun importAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            avatarRepository.importAvatar(uri).fold(
                onSuccess = {
                    _uiState.update { it.copy(isProcessing = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Import avatar failed: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun deleteAvatar(avatarId: String) {
        viewModelScope.launch {
            avatarRepository.deleteAvatar(avatarId).fold(
                onSuccess = { /* avatarsFlow will automatically update */ },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = "Delete avatar failed: ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    fun setDefaultAvatar(avatarId: String) {
        viewModelScope.launch {
            preferenceRepository.setSelectedDefaultAvatarId(avatarId)
        }
    }

    // --- Settings & Configuration ---

    fun updateEmojiList(emojis: String) {
        viewModelScope.launch {
            updateEmojiOptionsUseCase(emojis).fold(
                onSuccess = { },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to update emoji list: ${exception.localizedMessage}")
                    }
                }
            )
        }
    }

    fun toggleLauncherIcon(hideIcon: Boolean) {
        viewModelScope.launch {
            manageAppIconVisibilityUseCase(hideIcon).fold(
                onSuccess = { },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to toggle icon visibility: ${exception.localizedMessage}")
                    }
                }
            )
        }
    }

    fun copyFontToInternal(uri: Uri) {
        viewModelScope.launch {
            manageFontUseCase.addFont(uri).fold(
                onSuccess = { },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to add font: ${exception.localizedMessage}")
                    }
                }
            )
        }
    }

    fun removeFontFromInternal(filePath: String) {
        viewModelScope.launch {
            manageFontUseCase.removeFont(filePath).fold(
                onSuccess = { },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to remove font: ${exception.localizedMessage}")
                    }
                }
            )
        }
    }

    fun onFontSelected(selectedIndex: Int) {
        val selectedPath = _uiState.value.availableFontPaths.getOrNull(selectedIndex) ?: Constants.DEFAULT_FONT_MARKER
        if (selectedPath == _uiState.value.selectedFontPath) return

        viewModelScope.launch {
            manageFontUseCase.selectFont(selectedPath).fold(
                onSuccess = { },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Failed to select font: ${exception.localizedMessage}")
                    }
                }
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

    fun setMosaicTarget(target: Int) {
        viewModelScope.launch {
            preferenceRepository.setMosaicTarget(target)
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun generateFakeDetection(bitmapWidth: Int, bitmapHeight: Int): FakeDetection {
        val baseDimension = Random.nextInt(
            from = minOf(bitmapWidth, bitmapHeight) / 6,
            until = minOf(bitmapWidth, bitmapHeight) / 4
        )
        val variance = (baseDimension * 0.1f).toInt()
        val width = (baseDimension + Random.nextInt(-variance, variance + 1)).toFloat()
        val height = (baseDimension + Random.nextInt(-variance, variance + 1)).toFloat()
        val left = Random.nextInt(0, bitmapWidth - width.toInt()).toFloat()
        val top = Random.nextInt(0, bitmapHeight - height.toInt()).toFloat()

        val startAge = Random.nextInt(300, 990)
        val endAge = startAge + Random.nextInt(4, 21)

        return FakeDetection(
            box = android.graphics.RectF(left, top, left + width, top + height),
            label = "face",
            confidence = Random.nextDouble(0.95, 0.99).toFloat(),
            startAge = startAge,
            endAge = endAge
        )
    }
}