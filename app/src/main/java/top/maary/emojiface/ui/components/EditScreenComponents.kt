package top.maary.emojiface.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.launch
import top.maary.emojiface.R
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
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
    FloatingActionButton(onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.padding(8.dp),
        shape = MaterialShapes.Cookie7Sided.toShape()) {
        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiCard(emoji: String,
              onClick: () -> Unit,
              clickable: Boolean = true,
              fontFamily: FontFamily? = null,
              containerColor: Color,
              hPadding: Dp = 0.dp, vPadding: Dp = 16.dp) {
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(horizontal = hPadding, vertical = vPadding)
            .clip(MaterialShapes.Cookie4Sided.toShape())
            .background(containerColor)
            .clickable(enabled = clickable) { onClick() },  // 添加点击事件
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
fun ResultImg(modifier: Modifier, bitmap: ImageBitmap, description: String, ratio: Float, animate: Boolean) {
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
                    ))
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
        Text(modifier = Modifier.weight(1f), text = stringResource(R.string.hide_home))
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
                Text(tooltipText)
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
                contentDescription = "Show more information"
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
            Icon(Icons.Outlined.AttachFile, stringResource(R.string.choose_font))
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
    maxRange: Float
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
            valueRange = minRange..maxRange
        )

    }
}

@Composable
fun SettingsBottomSheetContent(
    emojiOptions: List<String>,
    isEditingEmojiList: Boolean,
    fontFamily: FontFamily?,
    isAppIconHidden: Boolean,
    availableFontNames: List<String>,
    selectedFontIndex: Int,
    onEditClick: () -> Unit,
    onEditConfirm: (newEmojiListString: String) -> Unit,
    onHideIconToggle: (hide: Boolean) -> Unit,
    onFontSelected: (index: Int) -> Unit,
    onAddFontClick: () -> Unit,
    onRemoveFontClick: (index: Int) -> Unit,
) {
    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        Column {
            Text(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
                text = stringResource(R.string.emoji_list))
            if (!isEditingEmojiList) {
                PredefinedEmojiSettings(
                    emojiOptions = emojiOptions,
                    onClick = onEditClick,
                    fontFamily = fontFamily)
            } else {
                EditEmojiList(
                    emojiOptions = emojiOptions,
                    onClick = onEditConfirm,
                    fontFamily = fontFamily)
            }
        }
    }
    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        HomeSwitchRow(state = isAppIconHidden, onCheckedChange = { onHideIconToggle(it) })

    }

    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        DropdownRow(
            options = availableFontNames.toMutableList(),
            position = selectedFontIndex,
            onItemClicked = onFontSelected,
            onAddClick = onAddFontClick,
            onRemoveClick = { onRemoveFontClick(it) })
    }

}

@Composable
fun SettingsSideSheetContent(
    emojiOptions: List<String>,
    isEditingEmojiList: Boolean,
    fontFamily: FontFamily?,
    isAppIconHidden: Boolean,
    availableFontNames: List<String>,
    selectedFontIndex: Int,
    onEditClick: () -> Unit,
    onEditConfirm: (newEmojiListString: String) -> Unit,
    onHideIconToggle: (hide: Boolean) -> Unit,
    onFontSelected: (index: Int) -> Unit,
    onAddFontClick: () -> Unit,
    onRemoveFontClick: (index: Int) -> Unit,
) {
    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        Column {
            Text(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
                text = stringResource(R.string.emoji_list))
            if (!isEditingEmojiList) {
                PredefinedEmojiSettings(
                    emojiOptions = emojiOptions,
                    onClick = onEditClick,
                    fontFamily = fontFamily)
            } else {
                EditEmojiList(
                    emojiOptions = emojiOptions,
                    onClick = onEditConfirm,
                    fontFamily = fontFamily)
            }
        }
    }
    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        HomeSwitchRow(state = isAppIconHidden, onCheckedChange = { onHideIconToggle(it) })

    }

    Box(modifier = Modifier
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .clip(
            RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 24.dp,
                bottomEnd = 24.dp
            )
        )
        .background(MaterialTheme.colorScheme.surfaceContainerLow)){
        DropdownRow(
            options = availableFontNames.toMutableList(),
            position = selectedFontIndex,
            onItemClicked = onFontSelected,
            onAddClick = onAddFontClick,
            onRemoveClick = { onRemoveFontClick(it) })
    }

}

@Composable
fun EditEmojiBottomSheetContent(
    initialEmoji: String,
    initialDiameter: Float,
    initialRotation: Float,
    maxDiameter: Float,
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    onValueChange: (emoji: String?, diameter: Float?, rotation: Float?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
){

    Column (modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Spacer(modifier = Modifier.height(24.dp))

        Row (verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                modifier = Modifier.width(96.dp),
                value = initialEmoji,
                onValueChange = { onValueChange(it, null, null) },
                label = { Text(stringResource(R.string.new_emoji))},
                textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 预置 emoji 选择行
            LazyRow(modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 8.dp)) {
                item {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                itemsIndexed(availableEmojis) { _, emoji ->
                    EmojiCardSmall(emoji = emoji, onClick = { onValueChange(emoji, null, null) }, fontFamily = fontFamily)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            SliderWithCaption(
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.FormatSize,
                        contentDescription = stringResource(R.string.emoji_size),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp))
                },
                description = stringResource(R.string.emoji_size),
                value = initialDiameter,
                onValueChange = { onValueChange(null, it, null) },
                minRange = 20f,
                maxRange = maxDiameter
            )

            Spacer(modifier = Modifier.width(8.dp))

            SliderWithCaption(
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Rotate90DegreesCw,
                        contentDescription = stringResource(R.string.emoji_angle),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp))
                },
                description = stringResource(R.string.emoji_angle),
                value = initialRotation,
                onValueChange = { onValueChange(null, null, it) },
                minRange = -90f,
                maxRange = 90f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { onDismiss() }) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(
                onClick = { onConfirm() }) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@Composable
fun EditEmojiSideSheetContent(
    initialEmoji: String,
    initialDiameter: Float,
    initialRotation: Float,
    maxDiameter: Float,
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    onValueChange: (emoji: String?, diameter: Float?, rotation: Float?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = { onConfirm() }) {
                    Text(stringResource(R.string.ok))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = initialEmoji,
            onValueChange = { onValueChange(it, null, null) },
            label = { Text(stringResource(R.string.new_emoji)) },
            textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
        ) }
        item {
            Spacer(modifier = Modifier.height(8.dp))

        }

        item {
            // 预置 emoji 选择行
            LazyRow(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                itemsIndexed(availableEmojis) { _, emoji ->
                    EmojiCardSmall(
                        emoji = emoji,
                        onClick = { onValueChange(emoji, null, null) },
                        fontFamily = fontFamily
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            SliderWithCaption(
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = stringResource(R.string.emoji_size),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_size),
                value = initialDiameter,
                onValueChange = { onValueChange(null, it, null) },
                minRange = 20f,
                maxRange = maxDiameter
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            SliderWithCaption(
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Rotate90DegreesCw,
                        contentDescription = stringResource(R.string.emoji_angle),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_angle),
                value = initialRotation,
                onValueChange = { onValueChange(null, null, it) },
                minRange = -90f,
                maxRange = 90f
            )
        }
    }
}

@Composable
fun EmojiOverlay(
    state: EditScreenState,
    editingEmoji: EmojiDetection?,
    padding: PaddingValues,
    cornerRadius: Dp,
) {
    // 获取原始图片尺寸和屏幕上容器的尺寸，用于坐标和大小的缩放
    val originalWidth = state.currentImage?.width?.toFloat() ?: return
    val originalHeight = state.currentImage.height.toFloat()
    val containerWidth = state.imageContainerSize.width.toFloat()
    val containerHeight = state.imageContainerSize.height.toFloat()

    if (containerWidth == 0f || containerHeight == 0f) return

    // 计算缩放比例
    val scale = containerWidth / originalWidth

    // 合并固定列表和正在编辑的临时状态，用于统一渲染
    val emojisToRender = remember(state.emojiDetections, editingEmoji) {
        val list = state.emojiDetections.toMutableList()
        val editingEmoji = editingEmoji

        if (editingEmoji != null) {
            val editingIndex = list.indexOfFirst { it.xCenter == editingEmoji.xCenter && it.yCenter == editingEmoji.yCenter }.takeIf { it != -1 }

            if (editingIndex != null) {
                // 原有逻辑：如果找到了（编辑模式），就替换它
                list[editingIndex] = editingEmoji
            } else {
                // 新增逻辑：如果没找到（新增模式），就把它添加到列表末尾
                list.add(editingEmoji)
            }
        }
        list
    }

    // 创建一个可以在重组间复用的 Paint 对象
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }
    }

    // 使用 Canvas Composable 进行绘制
    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .clip(RoundedCornerShape(cornerRadius))) {
        emojisToRender.forEach { detection ->
            // 从 state 获取加载好的原生 Typeface
            paint.typeface = state.typeface ?: Typeface.DEFAULT

            // 实时计算缩放后的大小
            paint.textSize = detection.diameter * scale

            // 计算垂直方向的偏移，与 RenderEmojiOnBitmapUseCase 完全一致
            val verticalOffset = (paint.descent() + paint.ascent()) / 2

            // 缩放坐标
            val centerX = detection.xCenter * scale
            val centerY = detection.yCenter * scale

            // 将所有绘制操作放在 rotate 的 lambda 块中
            // Compose 会自动处理 save 和 restore
            rotate(
                degrees = detection.angle,
                pivot = Offset(centerX, centerY)
            ) {
                // 在这个代码块中执行的所有绘制操作都会被旋转
                drawContext.canvas.nativeCanvas.drawText(
                    detection.emoji,
                    centerX,
                    centerY - verticalOffset,
                    paint
                )
            }
        }
    }
}

@Composable
fun DisplayPane(modifier: Modifier, state: EditScreenState, actions: EditScreenActions, editingEmoji: EmojiDetection?) {
    // --- 圖片顯示區域 ---
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (state.displayedBitmap != null) {

            val cornerRadius = 16.dp
            val verticalPadding = 8.dp
            // 水平 padding 依赖于宽高比，与 ResultImg 内部逻辑保持一致
            val horizontalPadding = (state.aspectRatio ?: 1f) * 8f.dp

            // 准备一个用于覆盖层的 Box，它的大小将和图片容器一致
            Box(
                modifier = Modifier
                    .aspectRatio(state.aspectRatio ?: 1f)
                    .fillMaxSize()
            ) {
                GlowingCard (
                    modifier = Modifier
                        .aspectRatio(state.aspectRatio ?: 1f) // 使用 state 中的寬高比
                        .fillMaxSize(),
                    ratio = state.aspectRatio ?: 1f,
                    animate = state.isProcessing,
                    cornersRadius = 16.dp,
                    content = {
                        Image(bitmap = state.displayedBitmap,
                            contentDescription = stringResource(R.string.process_result),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = (state.aspectRatio ?: 1f) * 8.dp,
                                    vertical = 8.dp
                                )
                                .clip(
                                    RoundedCornerShape(16.dp)
                                )
                                .onGloballyPositioned { layoutCoordinates ->
                                    // 回報容器尺寸
                                    actions.onImageContainerMeasured(layoutCoordinates.size)
                                }
                                .then(
                                    if (state.isAddMode) {
                                    Modifier.pointerInput(Unit) { // 合并 pointerInput

                                        detectTapGestures { offset ->
                                            // ... (原有的 onImageTapToAdd 逻辑保持不变)
                                            val containerWidth = state.imageContainerSize.width
                                            val containerHeight = state.imageContainerSize.height
                                            val originalBitmapWidth = state.currentImage?.width
                                                ?: state.displayedBitmap.width
                                            val originalBitmapHeight = state.currentImage?.height
                                                ?: state.displayedBitmap.height

                                            if (containerWidth > 0 && containerHeight > 0) {
                                                val scaleX =
                                                    originalBitmapWidth.toFloat() / containerWidth
                                                val scaleY =
                                                    originalBitmapHeight.toFloat() / containerHeight
                                                val originalX = offset.x * scaleX
                                                val originalY = offset.y * scaleY
                                                actions.onImageTapToAdd(
                                                    Offset(
                                                        originalX,
                                                        originalY
                                                    )
                                                )
                                            } else {
                                                actions.onImageTapToAdd(offset)
                                            }
                                        }

                                    }
                                } else Modifier
                                ))
                    }
                )

                EmojiOverlay(state = state, editingEmoji = editingEmoji,
                    padding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
                    cornerRadius = cornerRadius,)

            }

        } else {
            // 沒有圖片時顯示選擇圖片按鈕
            ExtendedFloatingActionButton(
                onClick = actions.onPickImageClick, // 使用 action
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