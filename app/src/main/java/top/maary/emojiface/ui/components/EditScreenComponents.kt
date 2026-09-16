package top.maary.emojiface.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TagFaces
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.maary.emojiface.BuildConfig
import top.maary.emojiface.R
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_AVATAR
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_BLUR
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_EMOJI
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TARGET_EYES
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TARGET_FACE
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TYPE_GAUSSIAN
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TYPE_HALFTONE
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TYPE_PIXELATED
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
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
fun ActionRow(state: EditScreenState, actions: EditScreenActions, isMediumLayout: Boolean) {
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
            if (state.displayedBitmap != null) {
                if (isMediumLayout) {
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
fun EasterEggRow(
    isTooDeep: Boolean,
    onTooDeepStateChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextContent(title = stringResource(R.string.too_deep), description = stringResource(R.string.too_deep_description))
        Spacer(modifier = Modifier.weight(1f))
        Tooltip(tooltipText = stringResource(R.string.just_kidding))
        Switch(checked = isTooDeep, onCheckedChange = onTooDeepStateChanged)
    }
}


@Composable
fun AboutRow(onEasterEggStateChanged: (Boolean) -> Unit) {
    var clickCount by remember { mutableIntStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    Row (modifier = Modifier.fillMaxWidth()
        .combinedClickable(
            onClick = {
                Log.e("AboutRow", "onClick: $clickCount")
                clickCount++
                if (clickCount == 1) {
                    job = scope.launch {
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
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MinHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.app_name),
                modifier = Modifier
            )
            Text(
                BuildConfig.VERSION_NAME,
                style = Typography.bodySmall,
            )
        }
    }
}

@Composable
fun TextContent(modifier: Modifier = Modifier, title: String, description: String) {
    Column(modifier = modifier){
        Text(
            title,
        )
        Text(
            description,
            style = Typography.bodySmall,
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
        stringResource(R.string.mosaic) to MOSAIC_MODE_BLUR,
        stringResource(R.string.avatar) to MOSAIC_MODE_AVATAR
    )

    Row (
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically){
        Text(
            text = stringResource(R.string.default_mosaic_mode),
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
fun MosaicTargetRow(
    selectedTarget: Int,
    onTargetSelected: (Int) -> Unit
){
    val mosaicTargets = listOf(
        Triple(MOSAIC_TARGET_FACE, Icons.Outlined.TagFaces, R.string.face),
        Triple(MOSAIC_TARGET_EYES, Icons.Outlined.Visibility, R.string.eyes),
    )

    Row (
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically){
        Text(
            text = stringResource(R.string.mosaic_target),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f)) // 占位符，推送文本到左侧
        mosaicTargets.forEachIndexed { index, (target, _, stringRes) ->
            ToggleButton (
                checked = selectedTarget == target,
                onCheckedChange = { if (it) onTargetSelected(target) },
                shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        mosaicTargets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = stringResource(stringRes))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MosaicTypeToolbar(
    selectedType: Int,                      // <-- 接收当前选中的类型
    onMosaicTypeSelected: (Int) -> Unit,    // <-- 接收类型选择的 Action
    onAddBlurRegionClick: () -> Unit
) {
    val mosaicTypes = listOf(
        Triple(MOSAIC_TYPE_GAUSSIAN, R.drawable.gaussian, R.string.gaussian),
        Triple(MOSAIC_TYPE_PIXELATED, R.drawable.pixelated, R.string.pixelated),
        Triple(MOSAIC_TYPE_HALFTONE, R.drawable.halftone, R.string.halftone)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        HorizontalFloatingToolbar(
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            expanded = true,
            content = {
                // 使用循环来创建按钮，代码更简洁且易于扩展
                mosaicTypes.forEachIndexed { index, (type, iconRes, stringRes) ->
                    // 根据是否被选中来决定按钮的外观
                    val isSelected = selectedType == type
                    val containerColor = if (isSelected) {
                        FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContentColor
                    } else {
                        FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContainerColor
                    }

                    val contentColor = if (isSelected) {
                        FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContainerColor
                    } else {
                        FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContentColor
                    }

                    FilledIconButton(
                        onClick = { onMosaicTypeSelected(type) }, // <-- 调用 Action
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(id = stringRes)
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onAddBlurRegionClick) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
                }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MosaicTypeBlock(
    selectedType: Int,                      // <-- 接收当前选中的类型
    onMosaicTypeSelected: (Int) -> Unit,    // <-- 接收类型选择的 Action
    onAddBlurRegionClick: () -> Unit
) {
    val mosaicTypes = listOf(
        Triple(MOSAIC_TYPE_GAUSSIAN, R.drawable.gaussian, R.string.gaussian),
        Triple(MOSAIC_TYPE_PIXELATED, R.drawable.pixelated, R.string.pixelated),
        Triple(MOSAIC_TYPE_HALFTONE, R.drawable.halftone, R.string.halftone)
    )
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .width(IntrinsicSize.Max)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,

            ) {

            // 使用循环来创建按钮，代码更简洁且易于扩展
            mosaicTypes.forEachIndexed { index, (type, iconRes, stringRes) ->
                // 根据是否被选中来决定按钮的外观
                val isSelected = selectedType == type
                val containerColor = if (isSelected) {
                    FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContentColor
                } else {
                    FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContainerColor
                }

                val contentColor = if (isSelected) {
                    FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContainerColor
                } else {
                    FloatingToolbarDefaults.standardFloatingToolbarColors().toolbarContentColor
                }

                Button(
                    onClick = { onMosaicTypeSelected(type) }, // <-- 调用 Action
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(id = stringRes)
                    )
                    Text(stringResource(stringRes))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ExtendedFloatingActionButton(
                onClick = onAddBlurRegionClick,
                containerColor = FloatingToolbarDefaults.vibrantFloatingToolbarColors().fabContainerColor,
                contentColor = FloatingToolbarDefaults.vibrantFloatingToolbarColors().fabContentColor
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add))
                Text(stringResource(R.string.add))
            }
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FaceThumbnailCard(
    occlusion: FaceOcclusion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    fontFamily: FontFamily? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(MaterialShapes.Cookie4Sided.toShape())
            .background(containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (val mode = occlusion.mode) {
            is OcclusionMode.Emoji -> {
                Text(
                    text = mode.emoji,
                    fontSize = 28.sp,
                    fontFamily = fontFamily,
                    modifier = Modifier.padding(4.dp)
                )
            }
            is OcclusionMode.Blur -> {
                val iconRes = when (mode.type) {
                    OcclusionMode.Blur.BlurType.Gaussian -> R.drawable.gaussian
                    OcclusionMode.Blur.BlurType.Pixelated -> R.drawable.pixelated
                    OcclusionMode.Blur.BlurType.Halftone -> R.drawable.halftone
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = stringResource(R.string.mosaic),
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is OcclusionMode.Avatar -> {
                val bitmap = cachedAvatarBitmaps[mode.avatarId]
                if (bitmap != null) {
                    val shape = when (mode.cropShape) {
                        OcclusionMode.Avatar.AvatarCropShape.Circle -> CircleShape
                        OcclusionMode.Avatar.AvatarCropShape.RoundedRect -> RoundedCornerShape(8.dp)
                        OcclusionMode.Avatar.AvatarCropShape.Square -> RoundedCornerShape(0.dp)
                    }
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.avatar),
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FaceAddCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(MaterialShapes.Cookie4Sided.toShape())
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.add),
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FaceSelectionBar(
    occlusions: List<FaceOcclusion>,
    onOcclusionClick: (index: Int) -> Unit,
    onOcclusionLongClick: (index: Int) -> Unit,
    onAddClick: () -> Unit,
    cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(
                items = occlusions,
                key = { _, item -> item.id }
            ) { index, occlusion ->
                FaceThumbnailCard(
                    modifier = Modifier.animateItem(),
                    occlusion = occlusion,
                    onClick = { onOcclusionClick(index) },
                    onLongClick = { onOcclusionLongClick(index) },
                    cachedAvatarBitmaps = cachedAvatarBitmaps,
                    fontFamily = fontFamily,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            item {
                FaceAddCard(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarManagementSheet(
    avatars: List<Avatar>,
    cachedAvatarBitmaps: Map<String, Bitmap>,
    defaultAvatarId: String,
    onAvatarClick: (avatarId: String) -> Unit,
    onAvatarDelete: (avatarId: String) -> Unit,
    onImportAvatarClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header Row: Title & Import Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.manage_avatars),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = onImportAvatarClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.import_avatar),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.import_avatar))
                }
            }

            if (avatars.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_avatars_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(avatars, key = { it.id }) { avatar ->
                        val isDefault = avatar.id == defaultAvatarId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAvatarClick(avatar.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDefault) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            border = if (isDefault) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = cachedAvatarBitmaps[avatar.id]
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = avatar.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountCircle,
                                            contentDescription = avatar.displayName,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = avatar.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isDefault) {
                                        Text(
                                            text = stringResource(R.string.default_avatar),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.set_as_default),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.clickable { onAvatarClick(avatar.id) }
                                        )
                                    }

                                    IconButton(
                                        onClick = { onAvatarDelete(avatar.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

