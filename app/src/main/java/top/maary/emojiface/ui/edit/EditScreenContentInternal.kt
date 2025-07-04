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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.collectLatest
import top.maary.emojiface.R
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

    // --- 1. Observe ViewModel's Single State Flow ---
    // Use collectAsStateWithLifecycle for better lifecycle awareness
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- 2. Hoist Remembered UI State (Local state managed within Composable) ---
    var showBottomSheet by remember { mutableStateOf(false) }
    var isAddMode by remember { mutableStateOf(false) }
    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndexForEdit by remember { mutableIntStateOf(-1) } // -1 for Add, >=0 for Edit index
    var tapPositionForAdd by remember { mutableStateOf(Offset.Zero) } // Store tap position for adding
    var isEditingEmojiListInSheet by remember { mutableStateOf(false) } // State for bottom sheet mode
    var isMediumLayout by remember { mutableStateOf(false) } // To pass to ActionRow if needed
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var emojiIndexToDelete by remember { mutableStateOf<Int?>(null) }

    // --- 3. Implement Launchers (No changes needed here) ---
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.detect(it) } // Call VM method
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.copyFontToInternal(it) } // Call VM method
    }

    // --- 4. Implement Effects ---

    // Handle incoming ACTION_SEND intent (No changes needed here)
    LaunchedEffect(activity?.intent) {
        val intent = activity?.intent
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val sharedUri: Parcelable? = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            (sharedUri as? Uri)?.let {
                // Check if already processed (using uiState.originalBitmap as indicator)
                if (uiState.originalBitmap == null) {
                    viewModel.detect(it)
                }
                intent.action = null // Prevent re-processing
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

                    // 统一显示 Toast
                    if (messageToShow.isNotEmpty()) {
                        Toast.makeText(context, messageToShow, Toast.LENGTH_SHORT).show()
                    }
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
        uiState.originalBitmap?.asImageBitmap()
    }
    val currentImageForUi = remember(uiState.originalBitmap) { // Needed? Only if layout explicitly needs original
        uiState.originalBitmap?.asImageBitmap()
    }
    val aspectRatio = remember(uiState.originalBitmap) {
        uiState.originalBitmap?.let {
            if (it.height > 0) it.width.toFloat() / it.height.toFloat() else 1f
        } ?: 1f // Default aspect ratio
    }
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
        currentImage = currentImageForUi, // Pass if needed by layout
        aspectRatio = aspectRatio,
        emojiDetections = uiState.selectedEmojis, // Direct from uiState
        predefinedEmojiList = uiState.predefinedEmojiOptions, // Direct from uiState
        fontFamily = uiState.loadedFontFamily, // Direct from uiState
        isAddMode = isAddMode, // Local UI state
        isProcessing = isProcessingForAnimation, // Use combined state for animation
        imageContainerSize = imageContainerSize, // Local UI state
        isAppIconHidden = uiState.isAppIconHidden, // Direct from uiState
        availableFontNames = fontNames, // Derived value
        selectedFontIndex = selectedFontIndex, // Derived value
        isMediumLayout = isMediumLayout, // Pass local state if needed by ActionRow etc.
        typeface = uiState.loadedTypeface, // Direct from uiState
        editingEmojiIndex = uiState.editingEmoji?.let { uiState.selectedEmojis.indexOf(it) }, // Get index of currently editing emoji
        isEasterEggEnabled = uiState.isEasterEggEnabled,
        isTooDeep = uiState.isTooDeep,
        fakeDetections = uiState.fakeDetections,
        mosaicMode = uiState.mosaicMode,
        blurRegions = uiState.blurRegions // 传递 mosaicMode
    )

    // --- 7. Create EditScreenActions Instance (Largely unchanged) ---
    // The public method names on ViewModel were kept the same
    val actions = remember(viewModel) { // Remember actions instance tied to VM
        EditScreenActions(
            onImageTapToAdd = { offset ->
                tapPositionForAdd = offset
                selectedIndexForEdit = -1 // Mark as Add
                isAddMode = false // Exit add mode state after tap
                viewModel.addEmoji(offset.x, offset.y, viewModel.getRandomEmoji(), 100f, 0f)
            },
            onImageContainerMeasured = { size -> imageContainerSize = size },
            onPickImageClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onClearImageClick = { viewModel.clearImage() },
            onEmojiCardClick = { index ->
                viewModel.startEditing(index)
            },
            onEmojiCardLongClick = { index ->
                emojiIndexToDelete = index
                showDeleteConfirmDialog = true
            },
            onAddEmojiCardClick = { isAddMode = true }, // Enter Add Mode
            onCloseClick = { activity?.finish() },
            // Share/Save now ignore the bitmap param internally in VM
            onShareClick = { viewModel.shareImage() },
            onSaveClick = { viewModel.saveImageToGallery() },
            onSettingsClick = { showBottomSheet = true },

            // Bottom Sheet Actions
            onSettingsSheetDismiss = {
                showBottomSheet = false
                isEditingEmojiListInSheet = false // Reset internal sheet state
            },
            onEditPredefinedEmojisClick = { isEditingEmojiListInSheet = true },
            onPredefinedEmojisEdited = { newEmojiListString ->
                viewModel.updateEmojiList(newEmojiListString)
                isEditingEmojiListInSheet = false
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
            // --- 实时编辑 Actions ---
            onEditingValueChanged = { emoji, diameter, rotation ->
                viewModel.updateEditingEmoji(emoji, diameter, rotation)
            },
            onConfirmEditing = { viewModel.confirmEditing() },
            onCancelEditing = { viewModel.cancelEditing() },
            onEasterEggStateChanged = { enabled -> viewModel.setEasterEggEnabled(enabled) },
            onTooDeepStateChanged = { enabled -> viewModel.setTooDeepEnabled(enabled) },
            onMosaicModeSelected = { mode -> viewModel.setMosaicMode(mode) }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                emojiIndexToDelete = null
            },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        emojiIndexToDelete?.let { viewModel.removeEmoji(it) }
                        showDeleteConfirmDialog = false
                        emojiIndexToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        emojiIndexToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


    // --- 8. Layout Dispatching ---
    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            isMediumLayout = false // Update layout flag
            LargeScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                editingEmoji = uiState.editingEmoji,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss)
        }
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            isMediumLayout = true // Update layout flag
            LargeScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                editingEmoji = uiState.editingEmoji,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss)
        }
        else -> {
            isMediumLayout = false // Update layout flag
            CompactScreenLayout(
                state = stateForUiLayout,
                actions = actions,
                editingEmoji = uiState.editingEmoji,
                showSettingsSheet = showBottomSheet,
                onDismissSettingsSheet = actions.onSettingsSheetDismiss)
        }
    }
}