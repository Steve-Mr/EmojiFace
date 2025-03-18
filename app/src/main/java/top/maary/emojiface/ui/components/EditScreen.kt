package top.maary.emojiface.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.maary.emojiface.EmojiDetection
import top.maary.emojiface.EmojiViewModel
import top.maary.emojiface.R

@Composable
fun ShareButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Default.Share, stringResource(R.string.share)) },
        text = { Text(text = stringResource(R.string.share)) })
}

@Composable
fun SaveButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Rounded.SaveAlt, stringResource(R.string.save)) },
        text = { Text(text = stringResource(R.string.save)) })
}

@Composable
fun EmojiCard(emojiDetection: EmojiDetection, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clickable { onClick() }  // 添加点击事件
    ) {
        Text(text = emojiDetection.emoji, fontSize = 60.sp)
    }
}


@Composable
fun EmojiRow(
    emojiDetections: List<EmojiDetection>,
    onEmojiClick: (Int, EmojiDetection) -> Unit
) {
    LazyRow {
        itemsIndexed(emojiDetections) { index, item ->
            EmojiCard(emojiDetection = item, onClick = { onEmojiClick(index, item) })
        }
    }
}


@Composable
fun ResultImg(modifier: Modifier, bitmap: ImageBitmap, description: String) {
    Image(bitmap = bitmap, contentDescription = description, modifier = modifier.padding(8.dp))
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? Activity
    val resultBitmap by emojiViewModel.outputBitmap.observeAsState()

    val emojiDetections by emojiViewModel.selectedEmojis.observeAsState(
        listOf(
            emojiViewModel.emptyEmojiDetection,
            emojiViewModel.emptyEmojiDetection,
            emojiViewModel.emptyEmojiDetection
        )
    )

    val currentImage by emojiViewModel.currentImage.observeAsState()

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

    // Photo Picker 相关
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            emojiViewModel.detect(it)
        }
    }

    // 控制弹窗显示的状态
    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    // 控制用户输入的新 emoji 和直径的状态
    var newEmoji by remember { mutableStateOf("") }
    var newDiameter by remember { mutableStateOf(0f) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
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
        }
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
                    .fillMaxWidth()
            ) {
                if (currentImage != null) {
                    ResultImg(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = (resultBitmap ?: currentImage!!).asImageBitmap(),
                        description = "处理结果"
                    )
                } else if (activity?.intent?.action != Intent.ACTION_SEND) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            icon = {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.choose_image),
                                )
                            },
                            text = { Text(text = stringResource(R.string.choose_image)) },
                        )
                    }
                }
            }
            EmojiRow(emojiDetections = emojiDetections, onEmojiClick = { index, detection ->
                // 点击某个 EmojiCard，打开对话框，同时初始化输入值
                selectedIndex = index
                newEmoji = detection.emoji
                newDiameter = detection.diameter
                showDialog = true
            })
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ShareButton {
                    // 获取当前显示的 Bitmap
                    val bitmap = resultBitmap?.asImageBitmap()?.asAndroidBitmap()
                    bitmap?.let { emojiViewModel.shareImage(it) }
                }
                SaveButton {
                    val bitmap = resultBitmap?.asImageBitmap()?.asAndroidBitmap()
                    bitmap?.let { emojiViewModel.saveImageToGallery(it) }
                }
            }
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }

    // 弹窗对话框：修改 emoji 和直径（可扩展为输入框和滑块）
// 弹窗对话框：修改 emoji 和直径（可扩展为输入框和滑块）
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("修改 Emoji") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newEmoji,
                        onValueChange = { newEmoji = it },
                        label = { Text("新 Emoji") }
                    )
                    // 新增：预置 emoji 选择行
                    LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                        itemsIndexed(emojiViewModel.emojiOptions) { _, emoji ->
                            Card(
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .padding(horizontal = 4.dp)
                                    .clickable { newEmoji = emoji }
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    Slider(
                        value = newDiameter,
                        onValueChange = { newDiameter = it },
                        valueRange = 20f..500f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedIndex >= 0) {
                        emojiViewModel.updateEmoji(selectedIndex, newEmoji, newDiameter)
                    }
                    showDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
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


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() {
//    Column (modifier = Modifier.fillMaxWidth()){
//        ResultImg(modifier = Modifier.weight(1f), painter = painterResource(R.drawable.test), description = "test")
//        EmojiRow(emojiList = listOf("😀","😃","😄","😁","😀","😃","😄","😁"))
//        Row(horizontalArrangement = Arrangement.SpaceBetween,
//            modifier = Modifier.fillMaxWidth()) {
//            ShareButton {  }
//            SaveButton {  }
//        }
//    }
}