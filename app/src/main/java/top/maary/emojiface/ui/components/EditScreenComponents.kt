package top.maary.emojiface.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.maary.emojiface.BuildConfig
import top.maary.emojiface.R
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_BLUR
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_EMOJI
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
import top.maary.emojiface.ui.theme.Typography
import top.maary.emojiface.util.Constants.DEFAULT_FONT_MARKER

@Composable
fun ShareButton(backgroundColor: Color, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Default.Share, stringResource(R.string.share)) },
        text = { Text(text = stringResource(R.string.share)) })
}

@Composable
fun SaveButton(backgroundColor: Color, onClick: () -> Unit) {
    ExtendedFloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp),
        icon = { Icon(Icons.Rounded.SaveAlt, stringResource(R.string.save)) },
        text = { Text(text = stringResource(R.string.save)) })
}

@Composable
fun ShareButtonCompact(backgroundColor: Color, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp)) {
        Icon(Icons.Default.Share, stringResource(R.string.share)) }
}

@Composable
fun SaveButtonCompact(backgroundColor: Color, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp)){
         Icon(Icons.Rounded.SaveAlt, stringResource(R.string.save))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsButton(backgroundColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 根据按压状态，驱动一个缩放比例的动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1.0f,
        label = "SettingsButtonScaleAnimation"
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        // 使用 graphicsLayer 修饰符来应用缩放
        modifier = Modifier
            .padding(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        // 在这里，我们可以安全地使用原始的 Cookie7Sided 形状
        shape = MaterialShapes.Cookie7Sided.toShape()
    ) {
        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiCard(
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier, // 新增 Modifier 参数
    onLongClick: () -> Unit = {},
    clickable: Boolean = true,
    fontFamily: FontFamily? = null,
    containerColor: Color,
    hPadding: Dp = 0.dp, vPadding: Dp = 16.dp
) {
    Box(
        modifier = modifier // 应用传入的 Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(horizontal = hPadding, vertical = vPadding)
            .clip(MaterialShapes.Cookie4Sided.toShape())
            .background(containerColor)
            .pointerInput(clickable) {
                if (clickable) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
            },
    ) {
        Text(text = emoji, fontSize = 40.sp, fontFamily = fontFamily, modifier = Modifier
            .padding(8.dp)
            .align(Alignment.Center))
    }
}

@Composable
fun EmojiCardSmall(emoji: String, onClick: () -> Unit, fontFamily: FontFamily? = null) {
    Card(
        modifier = Modifier
            .wrapContentHeight()
            .padding(end = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(8.dp),
            fontFamily = fontFamily
        )
    }
}

@Composable
fun ResultImg(
    modifier: Modifier,
    bitmap: ImageBitmap,
    description: String,
    ratio: Float,
    animate: Boolean,
    imageModifier: Modifier = Modifier
) {
    GlowingCard (
        modifier = modifier,
        ratio = ratio,
        animate = animate,
        cornersRadius = 16.dp,
        content = {
            Image(bitmap = bitmap,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ratio * 8.dp, vertical = 8.dp)
                    .clip(
                        RoundedCornerShape(16.dp)
                    )
                    // 使用 .then() 连接外部传入的 Modifier
                    .then(imageModifier)
            )
        }
    )
}

@Composable
fun PredefinedEmojiSettings(
    emojiOptions: List<String>,
    onClick: () -> Unit,
    fontFamily: FontFamily? = null
) {
    LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
        item { Spacer(modifier = Modifier
            .height(8.dp)
            .width(16.dp)) }
        itemsIndexed(emojiOptions) { _, emoji ->
            EmojiCardSmall(emoji = emoji, onClick = onClick, fontFamily = fontFamily)
        }
        item { Spacer(modifier = Modifier.size(8.dp)) }
    }
}

@Composable
fun EditEmojiList(emojiOptions: List<String>, onClick: (String) -> Unit, fontFamily: FontFamily? = null) {
    var text by remember { mutableStateOf(emojiOptions.joinToString(separator = "")) }
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        value = text,
        trailingIcon = {
            IconButton(onClick = { onClick(text) }) {
                Icon(Icons.Outlined.Done, stringResource(R.string.done))
            }
        },
        onValueChange = { text = it },
        textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
    )
}

@Composable
fun HomeSwitchRow(
    state: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!state) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.weight(1f), text = stringResource(R.string.hide_home), color = MaterialTheme.colorScheme.onSurface)
        Tooltip(tooltipText = stringResource(R.string.hide_home_bug))
        Switch(checked = state, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Tooltip(
    modifier: Modifier = Modifier,
    tooltipText: String
) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        modifier = modifier,
        positionProvider = rememberTooltipPositionProvider(),
        tooltip = {
            RichTooltip {
                Text(text = tooltipText, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        state = tooltipState
    ) {
        IconButton(onClick = { scope.launch {
            if (tooltipState.isVisible) {
                tooltipState.dismiss()
            } else {
                tooltipState.show()
            }
        } }) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Show more information",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownItem(
    modifier: Modifier,
    options: MutableList<String>,
    position: Int,
    onItemClicked: (Int) -> Unit,
    onItemActionClicked: (Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            modifier = Modifier.padding(8.dp),
            expanded = expanded,
            onExpandedChange = {
                if (options.size > 1) {
                    expanded = it
                } },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                value = if (options[position] == DEFAULT_FONT_MARKER) stringResource(R.string.default_font) else options[position],
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    if (options.size > 1) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                label = { Text(stringResource(R.string.emoji_font))}
            )
            ExposedDropdownMenu(
                modifier = Modifier.wrapContentWidth(),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        text = {
                            if (option == DEFAULT_FONT_MARKER) { Text(stringResource(R.string.default_font)) }
                            else { Text(option) } },
                        onClick = {
                            expanded = false
                            onItemClicked(options.indexOf(option))
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        trailingIcon = {
                            if (options.indexOf(option) != 0) {
                                IconButton(onClick = { onItemActionClicked(options.indexOf(option)) }) {
                                    Icon(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.remove_font))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DropdownRow(
    options: MutableList<String>,
    position: Int,
    onItemClicked: (Int) -> Unit,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DropdownItem(
            modifier = Modifier.weight(1f), options = options,
            position = position, onItemClicked = onItemClicked, onItemActionClicked = onRemoveClick
        )
        OutlinedIconButton(onClick = { onAddClick() }, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Outlined.AttachFile, stringResource(R.string.choose_font), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SliderWithCaption(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit),
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    minRange: Float,
    maxRange: Float,
    interactionSource: MutableInteractionSource
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon()
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Slider(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            value = value,
            onValueChange = onValueChange,
            valueRange = minRange..maxRange,
            interactionSource = interactionSource
        )

    }
}

@Composable
fun ActionRow(state: EditScreenState, actions: EditScreenActions) {
    Box(modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center // 將 Row 居中
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            // 這個 Row 只包含實際的按鈕
            horizontalArrangement = Arrangement.SpaceBetween, // 按鈕間距由 padding 或 Spacer 控制
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 只有在圖片已處理後才顯示分享和保存按鈕
            // (檢查 displayedBitmap 是否與 currentImage 不同，表示處理已完成)
            if (state.displayedBitmap != null && state.displayedBitmap != state.currentImage) {
                if (state.isMediumLayout) {
                    ShareButtonCompact(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        onClick = actions.onShareClick // 使用 action
                    )
                    SaveButtonCompact(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        onClick = actions.onSaveClick // 使用 action
                    )
                } else {
                    ShareButton(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        onClick = actions.onShareClick // 使用 action
                    )
                    SaveButton(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        onClick = actions.onSaveClick // 使用 action
                    )
                }
            }
        }
        SettingsButton(
            backgroundColor = MaterialTheme.colorScheme.tertiary,
            onClick = actions.onSettingsClick // 使用 action
        )
    }
}


@Composable
fun AboutRow(onEasterEggStateChanged: (Boolean) -> Unit) {
    var clickCount by remember {
        mutableIntStateOf(0)
    }
    var job by remember {
        mutableStateOf<Job?>(null)
    }
    Row (modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                Log.e("AboutRow", "onClick: $clickCount")
                clickCount++
                if (clickCount == 1) {
                    job = CoroutineScope(Dispatchers.Default).launch {
                        delay(5000) // 500 milliseconds
                        withContext(Dispatchers.Main) {
                            clickCount = 0
                        }
                    }
                } else if (clickCount == 5) {
                    job?.cancel()
                    clickCount = 0
                    onEasterEggStateChanged(true)
                    Log.e("AboutRow", "Easter egg activated!")
                }
            },
            onLongClick = {  onEasterEggStateChanged(false) })
        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)){
        TextContent(title = stringResource(id = R.string.app_name), description = BuildConfig.VERSION_NAME)
    }
}

@Composable
fun TextContent(modifier: Modifier = Modifier, title: String, description: String) {
    Column(modifier = modifier){
        Text(
            title,
            style = Typography.titleLarge
        )
        Text(
            description,
            style = Typography.bodySmall,
            maxLines = 5
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MosaicModeRow(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit
){
    val options = listOf(
        stringResource(R.string.emoji) to MOSAIC_MODE_EMOJI,
        stringResource(R.string.mosaic) to MOSAIC_MODE_BLUR
    )

    Row (
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically){
        Text(
            text = stringResource(R.string.mosaic_mode),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            style = Typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f)) // 占位符，推送文本到左侧
        options.forEachIndexed { index, (label, mode) ->
            ToggleButton (
                checked = selectedMode == mode ,
                onCheckedChange = { if (it) onModeSelected(mode) },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
            ) {
                Text(text = label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MosaicTypeToolbar(
    onAddBlurRegionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
//        FloatingActionButton(onClick = {}) { }
//        Spacer(modifier = Modifier.width(8.dp))
        HorizontalFloatingToolbar(
            expanded = true,
            content = {
                FilledIconButton(
                    modifier = Modifier.width(64.dp),
                    onClick = { /* doSomething() */ },
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Localized description")
                }
                FilledIconButton(
                    modifier = Modifier.width(64.dp),
                    onClick = { /* doSomething() */ },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Localized description")
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddBlurRegionClick) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
                }
            }
        )
    }

}

enum class GroupPosition {
    TOP,    // 顶部
    MIDDLE, // 中间
    BOTTOM, // 底部
    SINGLE  // 独立，自成一组
}

@Composable
fun SettingsItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit
) {
    // 根据 position 决定圆角形状
    val shape = when (position) {
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        GroupPosition.MIDDLE -> RoundedCornerShape(4.dp)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )

        GroupPosition.SINGLE -> RoundedCornerShape(24.dp) // 上下都是大圆角
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(shape) // 动态应用形状
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
