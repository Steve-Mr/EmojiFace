package top.maary.emojiface.ui.edit

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import kotlinx.coroutines.flow.collectLatest
import top.maary.emojiface.R
import top.maary.emojiface.ui.components.EditEmojiDialog
import top.maary.emojiface.ui.components.SettingsBottomSheetContent
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants
import top.maary.emojiface.util.getFileNameWithoutExtensionUsingPath
import top.maary.emojiface.util.getParcelableExtraCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreenContentInternal(
    viewModel: EmojiViewModel = viewModel(),
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- 1. Hoist ViewModel State ---
    val resultBitmapState by viewModel.outputBitmap.observeAsState()
    val currentImageState by viewModel.currentImage.observeAsState() // Bitmap?
    val emojiDetections by viewModel.selectedEmojis.observeAsState(emptyList())
    val predefinedEmojiList by viewModel.emojiList.observeAsState()
    val isAppIconHidden by viewModel.iconHideState.observeAsState(false)
    val availableFontPaths by viewModel.fontList.observeAsState() // List<String>? (paths)
    val selectedFontPath by viewModel.selectedFont.observeAsState()
    val fontFamily by viewModel.font.observeAsState() // FontFamily?

    // --- 2. Hoist Remembered UI State ---
    var showDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState() // For the bottom sheet
    var isAddMode by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) } // For animation/glow
    var imageContainerSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndexForEdit by remember { mutableIntStateOf(-1) } // -1 for Add, >=0 for Edit index
    var tapPositionForAdd by remember { mutableStateOf(Offset.Zero) } // Store tap position for adding
    var isEditingEmojiListInSheet by remember { mutableStateOf(false) } // State for bottom sheet mode

    // --- 3. Implement Launchers ---
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.detect(it) }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        // Consider adding explicit permission request if needed for SAF access
        uri?.let { viewModel.copyFontToInternal(it) }
    }

    // --- 4. Implement Effects ---
    // Handle incoming ACTION_SEND intent
    LaunchedEffect(activity?.intent) { // React to intent changes too if activity restarts
        val intent = activity?.intent
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val sharedUri: Parcelable? = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            (sharedUri as? android.net.Uri)?.let {
                // Avoid processing again if already processed (e.g., on config change)
                if (currentImageState == null) {
                    viewModel.detect(it)
                }
                // Clear the intent action to prevent re-processing on config change
                intent.action = null // Or handle more robustly with single event LiveData/Flow
            }
        }
    }

    // Collect share events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collectLatest { event -> // Use collectLatest or handle lifecycle correctly
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
                    Toast.makeText(
                        context,
                        context.getString(R.string.share_failed, event.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Update isProcessing state (for animation)
    LaunchedEffect(resultBitmapState, currentImageState, isAddMode) {
        // Animate if in add mode, or if processing (current image loaded but no result yet)
        isProcessing = isAddMode || (currentImageState != null && resultBitmapState == null)
    }

    // Back handler for Add Mode
    BackHandler(enabled = isAddMode) {
        isAddMode = false
        // isProcessing will be updated by the LaunchedEffect above
    }

    // --- 5. Create EditScreenState Instance ---
    val currentBitmap = currentImageState // Keep as Bitmap?
    val resultBitmap = resultBitmapState // Keep as Bitmap?
    val displayBitmapForUi = (resultBitmap ?: currentBitmap)?.asImageBitmap() // Convert for Image composable
    val aspectRatio = currentBitmap?.let {
        if (it.height > 0) it.width.toFloat() / it.height.toFloat() else 1f
    } ?: 1f

    val fontNames = availableFontPaths?.map { path ->
        when (path) {
            Constants.DEFAULT_FONT_MARKER -> context.getString(R.string.default_font)
            else -> getFileNameWithoutExtensionUsingPath(path)
        }
    }

    val selectedFontIndex = availableFontPaths?.indexOf(selectedFontPath) ?: 0

    var isMediumLayout by remember { mutableStateOf(false) }

    val state = EditScreenState(
        displayedBitmap = displayBitmapForUi,
        currentImage = currentBitmap?.asImageBitmap(), // Pass original as ImageBitmap if needed
        aspectRatio = aspectRatio,
        emojiDetections = emojiDetections,
        predefinedEmojiList = predefinedEmojiList,
        fontFamily = fontFamily,
        isAddMode = isAddMode,
        isProcessing = isProcessing,
        imageContainerSize = imageContainerSize,
        isAppIconHidden = isAppIconHidden,
        availableFontNames = fontNames,
        selectedFontIndex = selectedFontIndex,
        isMediumLayout = isMediumLayout
    )

    // --- 6. Create EditScreenActions Instance ---
    val actions = EditScreenActions(
        onImageTapToAdd = { offset ->
            tapPositionForAdd = offset // Store position for confirm action
            selectedIndexForEdit = -1 // Mark as Add
            isAddMode = false // Exit add mode state after tap
            showDialog = true // Open dialog to configure the new emoji
            // isProcessing will be updated by LaunchedEffect
        },
        onImageContainerMeasured = { size -> imageContainerSize = size },
        onPickImageClick = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onClearImageClick = { viewModel.clearImage() },
        onEmojiCardClick = { index ->
            selectedIndexForEdit = index // Mark as Edit with specific index
            showDialog = true
            // isProcessing will be updated by LaunchedEffect if needed
        },
        onAddEmojiCardClick = {
            // Enter Add Mode - wait for tap on image
            isAddMode = true
            // isProcessing will be updated by LaunchedEffect
        },
        onCloseClick = { activity?.finish() },
        onShareClick = { resultBitmapState?.let { viewModel.shareImage(it) } },
        onSaveClick = { resultBitmapState?.let { viewModel.saveImageToGallery(it) } },
        onSettingsClick = { showBottomSheet = true },

        // Dialog Actions
        onEditDialogConfirm = { newEmoji, newDiameter, newRotation ->
            if (selectedIndexForEdit >= 0) { // Was Editing
                viewModel.updateEmoji(selectedIndexForEdit, newEmoji, newDiameter, newRotation)
            } else { // Was Adding
                // Use the stored tapPositionForAdd
                viewModel.addEmoji(tapPositionForAdd.x, tapPositionForAdd.y, newEmoji, newDiameter, newRotation)
            }
            showDialog = false
            selectedIndexForEdit = -1 // Reset index after confirm
        },
        onEditDialogDismiss = {
            showDialog = false
            selectedIndexForEdit = -1 // Reset index
            if (isAddMode) { // If dialog was dismissed during add mode tap, exit add mode
                isAddMode = false
            }
            // isProcessing will be updated by LaunchedEffect
        },

        // Bottom Sheet Actions
        onSettingsSheetDismiss = {
            showBottomSheet = false
            isEditingEmojiListInSheet = false // Reset internal sheet state
        },
        onEditPredefinedEmojisClick = { isEditingEmojiListInSheet = true },
        onPredefinedEmojisEdited = { newEmojiListString ->
            viewModel.updateEmojiList(newEmojiListString)
            isEditingEmojiListInSheet = false // Exit editing mode in sheet
        },
        onHideIconToggle = { hide -> viewModel.toggleLauncherIcon(hide) },
        onFontSelected = { index -> viewModel.onFontSelected(index) }, // ViewModel handles index logic
        onAddFontClick = {
            filePicker.launch(arrayOf("application/octet-stream", "font/*")) // Common MIME types for fonts
        },
        onRemoveFontClick = { index ->
            // Get the actual font path from the original list using the index
            availableFontPaths?.getOrNull(index)?.let { fontPathToRemove ->
                if (fontPathToRemove != Constants.DEFAULT_FONT_MARKER) {
                    viewModel.removeFontFromInternal(fontPathToRemove)
                }
            }
        }
    )

    // --- 7. Layout Dispatching ---
    when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            LargeScreenLayout(state = state, actions = actions)
        }
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            isMediumLayout = true
            LargeScreenLayout(state = state, actions = actions)
        }
        else -> {
            CompactScreenLayout(state = state, actions = actions)
        }
    }

    // --- 8. Render Common UI (Dialogs, Bottom Sheets) ---
    if (showDialog) {
        val initialEmoji = if (selectedIndexForEdit >= 0) emojiDetections.getOrNull(selectedIndexForEdit)?.emoji else viewModel.getRandomEmoji()
        val initialDiameter = if (selectedIndexForEdit >= 0) emojiDetections.getOrNull(selectedIndexForEdit)?.diameter else 100f // Default size
        val initialRotation = if (selectedIndexForEdit >= 0) emojiDetections.getOrNull(selectedIndexForEdit)?.angle else 0f // Default angle

        EditEmojiDialog(
            initialEmoji = initialEmoji ?: "?",
            initialDiameter = initialDiameter ?: 100f,
            initialRotation = initialRotation ?: 0f,
            maxDiameter = (state.displayedBitmap?.let { minOf(it.width, it.height) / 3f } ?: 500f), // Dynamic max size
            availableEmojis = state.predefinedEmojiList ?: emptyList(),
            fontFamily = state.fontFamily,
            onConfirm = { emoji, diameter, rotation ->
                actions.onEditDialogConfirm(emoji, diameter, rotation)
            },
            onDismiss = actions.onEditDialogDismiss
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = actions.onSettingsSheetDismiss,
            sheetState = bottomSheetState
            // You can adjust windowInsets, scrimColor etc. if needed
        ) {
            // Embed the content composable
            SettingsBottomSheetContent(
                emojiOptions = state.predefinedEmojiList ?: emptyList(),
                isEditingEmojiList = isEditingEmojiListInSheet,
                fontFamily = state.fontFamily,
                isAppIconHidden = state.isAppIconHidden,
                availableFontNames = state.availableFontNames ?: listOf(stringResource(R.string.default_font)),
                selectedFontIndex = state.selectedFontIndex,
                onEditClick = actions.onEditPredefinedEmojisClick,
                onEditConfirm = actions.onPredefinedEmojisEdited,
                onHideIconToggle = actions.onHideIconToggle,
                onFontSelected = actions.onFontSelected,
                onAddFontClick = actions.onAddFontClick,
                onRemoveFontClick = actions.onRemoveFontClick
            )
        }
    }
}


