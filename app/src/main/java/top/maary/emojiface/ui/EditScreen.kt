package top.maary.emojiface.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.maary.emojiface.EmojiViewModel
import top.maary.emojiface.R
import top.maary.emojiface.ui.components.DropdownRow
import top.maary.emojiface.ui.components.EditEmojiList
import top.maary.emojiface.ui.components.EmojiCard
import top.maary.emojiface.ui.components.EmojiCardSmall
import top.maary.emojiface.ui.components.HomeSwitchRow
import top.maary.emojiface.ui.components.PredefinedEmojiSettings
import top.maary.emojiface.ui.components.ResultImg
import top.maary.emojiface.ui.components.SaveButton
import top.maary.emojiface.ui.components.SettingsButton
import top.maary.emojiface.ui.components.ShareButton
import top.maary.emojiface.ui.components.SliderWithCaption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val resultBitmap by emojiViewModel.outputBitmap.observeAsState()

    val emojiDetections by emojiViewModel.selectedEmojis.observeAsState(emptyList())

    val currentImage by emojiViewModel.currentImage.observeAsState()

    val emojiList by emojiViewModel.emojiList.observeAsState()

    LaunchedEffect(Unit) {
        val intent = activity?.intent
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val sharedUri: Uri? = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            sharedUri?.let {
                emojiViewModel.detect(it)
            }
        }
    }

    LaunchedEffect(Unit) {
        emojiViewModel.shareEvent.collect { event ->
            when (event) {
                is EmojiViewModel.ShareEvent.ShareImage -> {
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

                is EmojiViewModel.ShareEvent.Error -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.share_failed, event.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    var animationState by remember { mutableStateOf(false) }

    LaunchedEffect(resultBitmap) {
        animationState = resultBitmap == null
    }

    // Photo Picker 相关
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            emojiViewModel.detect(it)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            emojiViewModel.copyFontToInternal(it)
        }
    }

    // 控制弹窗显示的状态
    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // 控制用户输入的新 emoji 和直径的状态
    var newEmoji by remember { mutableStateOf("") }
    var newDiameter by remember { mutableFloatStateOf(0f) }
    var newRotation by remember { mutableFloatStateOf(0f) }

    // 新增状态：是否处于添加模式，以及记录点击坐标
    var isAddMode by remember { mutableStateOf(false) }
    var tapPosition by remember { mutableStateOf(Offset.Zero) }

    BackHandler(isAddMode) {
        isAddMode = false
        animationState = false
    }

    // 记住 Bottom Sheet 状态
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var isEditingEmojiList by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val hideAppIcon by emojiViewModel.iconHideState.observeAsState(false)

    val fontList by emojiViewModel.fontList.observeAsState()
    val selectedFont by emojiViewModel.selectedFont.observeAsState()

    val fontFamily by emojiViewModel.font.observeAsState()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.shadow(8.dp),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.tertiary,
                ),
                title = {
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.exit)
                        )
                    }
                },
                actions = {
                    if (currentImage != null) {
                        IconButton(onClick = { emojiViewModel.clearImage() }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_photo)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            // 图片显示区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (currentImage != null) {
                    ResultImg(
                        modifier = Modifier
                            .aspectRatio(currentImage!!.width.toFloat() / currentImage!!.height.toFloat())
                            .fillMaxSize()
                            .onGloballyPositioned { imageSize = it.size }
                            .then(
                                if (isAddMode) Modifier.pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val baseBitmap = resultBitmap ?: currentImage
                                        if (baseBitmap != null && imageSize.width > 0 && imageSize.height > 0) {
                                            val scaleX =
                                                baseBitmap.width.toFloat() / imageSize.width
                                            val scaleY =
                                                baseBitmap.height.toFloat() / imageSize.height
                                            // 转换后的坐标就是原图坐标
                                            val originalX = offset.x * scaleX
                                            val originalY = offset.y * scaleY
                                            tapPosition = Offset(originalX, originalY)
                                        }
                                        newEmoji = emojiViewModel.getRandomEmoji()
                                        newDiameter = 100f
                                        newRotation = 0f
                                        showDialog = true
                                        isAddMode = false
                                    }
                                } else Modifier
                            ),
                        bitmap = (resultBitmap ?: currentImage!!).asImageBitmap(),
                        description = stringResource(R.string.process_result),
                        animate = animationState,
                        ratio = currentImage!!.width.toFloat() / currentImage!!.height.toFloat()
                    )
                } else {
                        ExtendedFloatingActionButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.choose_image)
                                )
                            },
                            text = { Text(text = stringResource(R.string.choose_image)) },
                        )
                    }
            }
            LazyRow {
                itemsIndexed(emojiDetections) { index, detection ->
                    EmojiCard(emoji = detection.emoji, onClick = {
                        // 点击某个 EmojiCard，打开对话框，同时初始化输入值
                        selectedIndex = index
                        newEmoji = detection.emoji
                        newDiameter = detection.diameter
                        newRotation = detection.angle
                        showDialog = true
                    }, fontFamily = fontFamily, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
                item {
                    if (currentImage !=null ){
                        EmojiCard(emoji = "➕", onClick = {
                            selectedIndex = -1
                            isAddMode = true
                            animationState = true
                        }, clickable = true, fontFamily = fontFamily, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    }

                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (resultBitmap != null) {
                        ShareButton(backgroundColor = MaterialTheme.colorScheme.secondary) {
                            // 获取当前显示的 Bitmap
                            val bitmap = resultBitmap?.asImageBitmap()?.asAndroidBitmap()
                            bitmap?.let { emojiViewModel.shareImage(it) }
                        }
                        SaveButton(backgroundColor = MaterialTheme.colorScheme.secondary) {
                            val bitmap = resultBitmap?.asImageBitmap()?.asAndroidBitmap()
                            bitmap?.let { emojiViewModel.saveImageToGallery(it) }
                        }
                    }

                }
                SettingsButton(backgroundColor = MaterialTheme.colorScheme.tertiary) {
                    showBottomSheet = true
                }
            }
        }
    }

    // 弹窗对话框：修改 emoji 和直径（可扩展为输入框和滑块）
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.change_emoji)) },
            text = {
                // Calculate dynamic range for the slider based on imageSize
                val maxDiameter = remember(imageSize) {
                    if (imageSize.width > 0 && imageSize.height > 0) {
                        minOf(resultBitmap!!.width, resultBitmap!!.height) / 5f // Adjust the divisor as needed
                    } else {
                        500f // Default max value if image size is not yet available
                    }
                }
                Column {
                    OutlinedTextField(
                        value = newEmoji,
                        onValueChange = { newEmoji = it },
                        label = { Text(stringResource(R.string.new_emoji))},
                        textStyle = TextStyle(fontFamily = fontFamily)
                    )
                    // 预置 emoji 选择行
                    LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                        itemsIndexed(emojiList!!) { _, emoji ->
                            EmojiCardSmall(emoji = emoji, onClick = { newEmoji = emoji }, fontFamily = fontFamily)
                        }
                    }
                    SliderWithCaption(
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.FormatSize,
                                contentDescription = stringResource(R.string.emoji_size),
                                modifier = Modifier.padding(8.dp).size(24.dp))
                        },
                        description = stringResource(R.string.emoji_size),
                        value = newDiameter,
                        onValueChange = { newDiameter = it },
                        minRange = 20f,
                        maxRange = maxDiameter
                    )
                    SliderWithCaption(
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Rotate90DegreesCw,
                                contentDescription = stringResource(R.string.emoji_angle),
                                modifier = Modifier.padding(8.dp).size(24.dp))
                        },
                        description = stringResource(R.string.emoji_angle),
                        value = newRotation,
                        onValueChange = { newRotation = it },
                        minRange = -90f,
                        maxRange = 90f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedIndex >= 0) {
                        emojiViewModel.updateEmoji(selectedIndex, newEmoji, newDiameter, newRotation)
                    } else {
                        // 新增 emoji，使用之前记录的 tapPosition
                        emojiViewModel.addEmoji(tapPosition.x, tapPosition.y, newEmoji, newDiameter, newRotation)
                    }
                    showDialog = false
                    selectedIndex = -1
                    animationState = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    animationState = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                isEditingEmojiList = false
                showBottomSheet = false },
            sheetState = sheetState
        ) {
            if (!isEditingEmojiList) {
                PredefinedEmojiSettings(
                    emojiOptions = emojiList!!,
                    onClick = {
                        isEditingEmojiList = true
                    },
                    fontFamily = fontFamily)
            } else {
                EditEmojiList(
                    emojiOptions = emojiList!!,
                    onClick = {
                        emojiViewModel.updateEmojiList(it)
                        isEditingEmojiList = false
                    },
                    fontFamily = fontFamily)
            }
            HomeSwitchRow(state = hideAppIcon, onCheckedChange = { isChecked ->
                emojiViewModel.toggleLauncherIcon(isChecked)
            })
            DropdownRow(
                options = (fontList!!.map { font ->
                    emojiViewModel.getFileNameWithoutExtensionUsingPath(font) }).toMutableList(),
                position = fontList!!.indexOf(selectedFont),
                onItemClicked = { emojiViewModel.onFontSelected(it) },
                onAddClick = { filePicker.launch(arrayOf("application/octet-stream", "font/*")) },
                onRemoveClick = { emojiViewModel.removeFontFromInternal(fontList!![it])})
        }
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name) as? T
    }
}
