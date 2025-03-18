package top.maary.emojiface.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
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
fun EmojiCard(emoji: String) {
    Card(modifier =
    Modifier.wrapContentHeight().padding(horizontal = 8.dp, vertical = 16.dp)) {
        Text(text = emoji, fontSize = 60.sp)
    }
}

@Composable
fun EmojiRow(emojiList: List<String>) {
    LazyRow { for (emoji: String in emojiList) item { EmojiCard(emoji = emoji) } }
}

@Composable
fun ResultImg(modifier: Modifier, bitmap: ImageBitmap, description: String){
    Image(bitmap = bitmap, contentDescription = description, modifier = modifier.padding(8.dp))
}

@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel()) {
    val context = LocalContext.current

    // 观察 ViewModel 中的 LiveData，检测完成后返回处理后的 Bitmap
    val resultBitmap by emojiViewModel.outputBitmap.observeAsState()
    // 观察 ViewModel 中的 LiveData，检测完成后返回选取的 emoji 顺序
    val emojiList by emojiViewModel.selectedEmojis.observeAsState(listOf("⏳", "⏳", "⏳"))

    // 从资源中加载测试图片
    val testBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.test)
    }

    // 在组合函数启动时调用 detect 方法进行检测
    LaunchedEffect(Unit) {
        emojiViewModel.detect(testBitmap)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            // 如果检测结果尚未返回，则显示测试图片；否则显示处理后的结果
            ResultImg(
                modifier = Modifier.weight(1f),
                bitmap = (resultBitmap ?: testBitmap).asImageBitmap(),
                description = "检测结果"
            )
            // 使用 ViewModel 中的 emoji 列表展示 EmojiRow
            EmojiRow(emojiList = emojiList)
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