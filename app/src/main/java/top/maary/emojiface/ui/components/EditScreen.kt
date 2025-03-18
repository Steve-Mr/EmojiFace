package top.maary.emojiface.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.maary.emojiface.EmojiDetection
import top.maary.emojiface.EmojiViewModel
import top.maary.emojiface.R

@Composable
fun ShareButton(onClick: () -> Unit){
    ExtendedFloatingActionButton(onClick = onClick, modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Default.Share, stringResource(R.string.share)) },
        text = { Text(text = stringResource(R.string.share)) })
}

@Composable
fun SaveButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick, modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Rounded.SaveAlt, stringResource(R.string.save)) },
        text = { Text(text = stringResource(R.string.save)) } )
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
fun ResultImg(modifier: Modifier, bitmap: ImageBitmap, description: String){
    Image(bitmap = bitmap, contentDescription = description, modifier = modifier.padding(8.dp))
}

@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel()) {
    val context = LocalContext.current
    val resultBitmap by emojiViewModel.outputBitmap.observeAsState()
    val emptyEmojiDetection = EmojiDetection(xCenter = 0f, yCenter = 0f, diameter = 0f, angle = 0f, emoji = "⏳")
    val emojiDetections by emojiViewModel.selectedEmojis.observeAsState(listOf(emptyEmojiDetection, emptyEmojiDetection, emptyEmojiDetection))
    val testBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.test2)
    }

    // 控制弹窗显示的状态
    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    // 控制用户输入的新 emoji 和直径的状态
    var newEmoji by remember { mutableStateOf("") }
    var newDiameter by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        emojiViewModel.detect(testBitmap)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            ResultImg(
                modifier = Modifier.weight(1f),
                bitmap = (resultBitmap ?: testBitmap).asImageBitmap(),
                description = "检测结果"
            )
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
                ShareButton { }
                SaveButton { }
            }
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }

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
                    // 这里可以添加 Slider 控件用于调整 newDiameter
                Slider(value = newDiameter, onValueChange = { newDiameter = it }, valueRange = 20f..500f)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 调用 ViewModel 更新函数
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