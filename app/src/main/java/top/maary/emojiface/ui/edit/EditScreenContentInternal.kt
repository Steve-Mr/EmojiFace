package top.maary.emojiface.ui.edit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.collectLatest
import top.maary.emojiface.MainActivity
import top.maary.emojiface.R
import top.maary.emojiface.ui.components.AvatarManagementSheet
import top.maary.emojiface.ui.components.ReplaceImageBottomSheet
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getFileNameWithoutExtensionUsingPath
import top.maary.emojiface.util.getParcelableExtraCompat

@Composable
fun EditScreenContentInternal(
    // Assuming Hilt provides the ViewModel with UseCases injected
    viewModel: EmojiViewModel = viewModel(),
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mainActivity = activity as? MainActivity

    // --- 1. Observe ViewModel's Single State Flow ---
    // Use collectAsStateWithLifecycle for better lifecycle awareness
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- 2. Hoist Remembered UI State (Local state managed within Composable) ---
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAvatarManagementSheet by remember { mutableStateOf(false) }
    var isAddMode by remember { mutableStateOf(false) }
    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var occlusionIndexToDelete by remember { mutableStateOf<Int?>(null) }

    // Use rememberSaveable to persist the uri across rotation
    var newSharedUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val newSharedUri = remember(newSharedUriString) { newSharedUriString?.toUri() }

    var pickerLaunchedOnMain by remember { mutableStateOf(false) }

    // --- 3. Implement Launchers ---
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.detect(it) }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.copyFontToInternal(it) } // Call VM method
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.detect(it) }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.importAvatar(it) }
    }

    val avatarGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAvatar(it) }
    }

    val onImportAvatarClick = {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
            avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            avatarGetContentLauncher.launch("image/*")
        }
    }

    LaunchedEffect(activity, uiState.originalBitmap, pickerLaunchedOnMain) {
        val intent = activity?.intent
        // 检查三个条件：
        // 1. 是不是从桌面启动 (ACTION_MAIN)
        // 2. 当前是不是还没有图片 (originalBitmap == null)
        // 3. 是不是还没有触发过这个逻辑 (pickerLaunchedOnMain == false)
        if (intent?.action == Intent.ACTION_MAIN && uiState.originalBitmap == null && !pickerLaunchedOnMain) {
            // 标记为已触发，防止重组时再次执行
            pickerLaunchedOnMain = true
            // 启动图片选择器
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                // 如果可用，启动现代 PhotoPicker
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                // 如果不可用，启动回退方案
                getContentLauncher.launch("image/*")
            }
        }
    }

    // --- 4. Implement Effects ---

    // Handle incoming ACTION_SEND intent initially
    LaunchedEffect(activity?.intent) {
        val intent = activity?.intent
        // Only trigger if we have an intent and we haven't already shown a prompt for it.
        // We use newSharedUri as a guard to not show the sheet multiple times on rotation.
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true && newSharedUri == null) {
            val sharedUri: Parcelable? = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            (sharedUri as? Uri)?.let { uri ->
                if (uiState.originalBitmap == null) {
                    viewModel.detect(uri)
                } else {
                    newSharedUriString = uri.toString()
                }
                intent.action = null // Prevent re-processing on rotation
            }
        }
    }

    LaunchedEffect(mainActivity) {
        mainActivity?.newIntents?.collectLatest { intent ->
            if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
                val sharedUri: Parcelable? = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
                (sharedUri as? Uri)?.let { uri ->
                    if (uiState.originalBitmap == null) {
                        viewModel.detect(uri)
                    } else {
                        newSharedUriString = uri.toString()
                    }
                    intent.action = null
                }
            }
        }
    }

    // Collect share/event flow from ViewModel (No changes needed here, assuming ShareEvent exists)
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collectLatest { event ->
            when (event) {
                is ShareEvent.ShareImage -> {
                    val shareIntent = Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, event.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        context.getString(R.string.share)
                    )
                    context.startActivity(shareIntent)
                }
                is ShareEvent.Error -> {
                    // 使用 when 表达式来确定消息
                    val messageToShow: String = when (event.status) {
                        Constants.STATUS_SHARE -> context.getString(R.string.share_failed, event.message)
                        Constants.STATUS_SAVE -> context.getString(R.string.save_failed, event.message)
                        else -> {
                            // 对于其他所有状态，直接使用 event.message
                            // 使用 Elvis 操作符处理可能的 null 情况
                            event.message
                        }
                    }

                    Toast.makeText(context, messageToShow, Toast.LENGTH_SHORT).show()
                }
                is ShareEvent.Success -> { // Handle potential success messages
                    if (event.status == Constants.STATUS_SAVE) {
                        Toast.makeText(context, R.string.save_success, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Handle ViewModel error messages
    val currentErrorMessage = uiState.errorMessage
    LaunchedEffect(currentErrorMessage) {
        if (currentErrorMessage != null) {
            Toast.makeText(context, currentErrorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage() // Acknowledge error
        }
    }

    // Back handler for Add Mode (No changes needed here)
    BackHandler(enabled = isAddMode) {
        isAddMode = false
    }

    // --- 5. Derive UI-Specific Values from uiState ---
    // Use remember to avoid recalculating on every recomposition unless inputs change
    val displayedBitmapForUi = remember(uiState.originalBitmap) {
        uiState.displayedBitmap?.asImageBitmap()
    }

    val aspectRatio = uiState.aspectRatio ?: 1f

    val fontNames = remember(uiState.availableFontPaths) {
        uiState.availableFontPaths.map { path ->
            when (path) {
                Constants.DEFAULT_FONT_MARKER -> context.getString(R.string.default_font)
                else -> getFileNameWithoutExtensionUsingPath(path)
            }
        }
    }
    val selectedFontIndex = remember(uiState.selectedFontPath, uiState.availableFontPaths) {
        uiState.availableFontPaths.indexOf(uiState.selectedFontPath).coerceAtLeast(0) // Ensure non-negative
    }
    // Combine ViewModel's processing state with UI's add mode for animation trigger
    val isProcessingForAnimation = remember(uiState.isProcessing, uiState.isRendering, isAddMode) {
        derivedStateOf { uiState.isProcessing || uiState.isRendering || isAddMode }
    }.value


    // --- 6. Create EditScreenState Instance for Layouts ---
    // Combine derived values, ViewModel state, and local UI state
    val stateForUiLayout = EditScreenState(
        displayedBitmap = displayedBitmapForUi,
        aspectRatio = aspectRatio,
        occlusions = uiState.occlusions,
        editingOcclusionIndex = uiState.editingOcclusionIndex,
        editingOcclusion = uiState.editingOcclusion,
        availableAvatars = uiState.availableAvatars,
        cachedAvatarBitmaps = uiState.cachedAvatarBitmaps,
        defaultMosaicMode = uiState.defaultMosaicMode,
        predefinedEmojiList = uiState.predefinedEmojiOptions,
        fontFamily = uiState.loadedFontFamily,
        typeface = uiState.loadedTypeface,
        isAddMode = isAddMode,
        isProcessing = isProcessingForAnimation,
        imageContainerSize = imageContainerSize,
        isAppIconHidden = uiState.isAppIconHidden,
        availableFontNames = fontNames,
        selectedFontIndex = selectedFontIndex,
        isEasterEggEnabled = uiState.isEasterEggEnabled,
        isTooDeep = uiState.isTooDeep,
        fakeDetections = uiState.fakeDetections,
        mosaicMode = uiState.mosaicMode,
        mosaicType = uiState.mosaicType,
        mosaicTarget = uiState.mosaicTarget,
        isSliding = uiState.isSliding,
        isEditMode = uiState.isEditMode,
        shakeEditButtonTrigger = uiState.shakeEditButtonTrigger
    )

    // --- 7. Create EditScreenActions Instance ---
    val actions = remember(viewModel) { // Remember actions instance tied to VM
        EditScreenActions(
            onImageTapToAdd = { offset ->
                isAddMode = false // Exit add mode state after tap
                viewModel.addItemAtPosition(offset.x, offset.y)
            },
            onImageContainerMeasured = { size -> imageContainerSize = size },
            onPickImageClick = {
                if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                    // 如果可用，启动现代 PhotoPicker
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    // 如果不可用，启动回退方案
                    getContentLauncher.launch("image/*")
                }
            },
            onClearImageClick = { viewModel.clearImage() },

            onAddClicked = { isAddMode = true }, // Enter Add Mode
            onOcclusionSelected = { index ->
                viewModel.startEditingOcclusion(index)
            },
            onOcclusionLongClick = { index ->
                occlusionIndexToDelete = index
                showDeleteConfirmDialog = true
            },
            onDeleteOcclusion = { index ->
                occlusionIndexToDelete = index
                showDeleteConfirmDialog = true
            },

            onCloseClick = { activity?.finish() },
            onShareClick = { viewModel.shareImage() },
            onSaveClick = { viewModel.saveImageToGallery() },
            onSettingsClick = { showBottomSheet = true },

            // --- Unified Editing Operations ---
            onOcclusionModeChange = { newMode -> viewModel.updateEditingMode(newMode) },
            onOcclusionSizeChange = { factor -> viewModel.updateEditingGeometry(scaleFactor = factor) },
            onOcclusionAngleChange = { angle -> viewModel.updateEditingGeometry(angle = angle) },
            onConfirmEditing = { viewModel.confirmEditing() },
            onCancelEditing = { viewModel.cancelEditing() },

            // --- Avatar Management Operations ---
            onManageAvatarsClick = { showAvatarManagementSheet = true },
            onAvatarImported = { uri -> viewModel.importAvatar(uri) },
            onAvatarDeleted = { avatarId -> viewModel.deleteAvatar(avatarId) },
            onAvatarDefaultSet = { avatarId -> viewModel.setDefaultAvatar(avatarId) },

            // Bottom Sheet Actions
            onSettingsSheetDismiss = {
                showBottomSheet = false
            },
            onPredefinedEmojisEdited = { newEmojiListString ->
                viewModel.updateEmojiList(newEmojiListString)
            },
            onHideIconToggle = { hide -> viewModel.toggleLauncherIcon(hide) },
            onFontSelected = { index -> viewModel.onFontSelected(index) },
            onAddFontClick = {
                filePicker.launch(arrayOf("application/octet-stream", "font/*"))
            },
            onRemoveFontClick = { index ->
                // Get the actual font path using the index from uiState
                uiState.availableFontPaths.getOrNull(index)?.let { fontPathToRemove ->
                    if (fontPathToRemove != Constants.DEFAULT_FONT_MARKER) {
                        viewModel.removeFontFromInternal(fontPathToRemove)
                    }
                }
            },
            onEasterEggStateChanged = { enabled -> viewModel.setEasterEggEnabled(enabled) },
            onTooDeepStateChanged = { enabled -> viewModel.setTooDeepEnabled(enabled) },
            onMosaicModeSelected = { mode -> viewModel.setMosaicMode(mode) },
            onMosaicTypeSelected = { type -> viewModel.setMosaicType(type) },
            onMosaicTargetSelected = { target -> viewModel.setMosaicTarget(target) },
            onSlidingStateChange = { isSliding -> viewModel.onSlidingStateChanged(isSliding) },
            onToggleEditMode = { viewModel.toggleEditMode() },
            onSetEditMode = { viewModel.setEditMode(it) },
            onTriggerEditButtonShake = { viewModel.triggerEditButtonShake() }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                occlusionIndexToDelete = null
            },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        occlusionIndexToDelete?.let {
                            viewModel.removeOcclusion(it)
                            viewModel.cancelEditing()
                        }
                        showDeleteConfirmDialog = false
                        occlusionIndexToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        occlusionIndexToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAvatarManagementSheet) {
        AvatarManagementSheet(
            avatars = uiState.availableAvatars,
            cachedAvatarBitmaps = uiState.cachedAvatarBitmaps,
            defaultAvatarId = uiState.selectedDefaultAvatarId,
            onAvatarClick = { avatarId ->
                actions.onAvatarDefaultSet(avatarId)
            },
            onAvatarDelete = { avatarId ->
                actions.onAvatarDeleted(avatarId)
            },
            onImportAvatarClick = onImportAvatarClick,
            onDismissRequest = { showAvatarManagementSheet = false }
        )
    }

    ReplaceImageBottomSheet(
        newUri = newSharedUri,
        onDismissRequest = { newSharedUriString = null },
        onConfirm = {
            newSharedUri?.let { viewModel.detect(it) }
        }
    )

    // --- 8. Layout Dispatching ---
    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            LargeScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss,
                isMediumLayout = false)
        }
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            LargeScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss,
                isMediumLayout = true)
        }
        else -> {
            CompactScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss)
        }
    }
}
