package top.maary.emojiface.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.maary.emojiface.R
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState

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
    isEasterEggEnabled: Boolean,
    onEasterEggStateChanged: (Boolean) -> Unit,
    isTooDeep: Boolean,
    onTooDeepStateChanged: (Boolean) -> Unit,
    mosaicMode: Int,
    onMosaicModeSelected: (Int) -> Unit,
    mosaicTarget: Int,
    onMosaicTargetSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.animateContentSize() // ✨ 添加这一行
    ) {
        item {
            SettingsItem(GroupPosition.TOP) {
                MosaicModeRow(
                    selectedMode = mosaicMode,
                    onModeSelected = onMosaicModeSelected
                )
            }
        }
        item {
            AnimatedContent(
                targetState = mosaicMode,
                label = "MosaicModeAnimation",
                transitionSpec = {
                    // 定义动画规格
                    // 退出动画：向下滑出 + 淡出，持续 300ms
                    val exitTransition = slideOutVertically { height -> height } + fadeOut(
                        animationSpec = tween(300)
                    )
                    // 进入动画：从下方滑入 + 淡入，持续 300ms，但延迟 300ms 开始
                    val enterTransition = slideInVertically { height -> height } + fadeIn(
                        animationSpec = tween(300, delayMillis = 300)
                    )

                    // 将进入和退出动画组合起来，并应用平滑的尺寸变化
                    (enterTransition togetherWith exitTransition).using(
                        // SizeTransform 确保容器尺寸平滑变化，不会出现内容被裁剪或跳变的问题
                        SizeTransform(clip = true)
                    )
                }
            ) { targetMode ->
                if (targetMode == PreferenceRepository.MOSAIC_MODE_EMOJI) {
                    Column {
                        SettingsItem(position = GroupPosition.MIDDLE) {
                            Column {
                                Text(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = 0.dp
                                    ),
                                    text = stringResource(R.string.emoji_list)
                                )
                                if (!isEditingEmojiList) {
                                    PredefinedEmojiSettings(
                                        emojiOptions = emojiOptions,
                                        onClick = onEditClick,
                                        fontFamily = fontFamily
                                    )
                                } else {
                                    EditEmojiList(
                                        emojiOptions = emojiOptions,
                                        onClick = onEditConfirm,
                                        fontFamily = fontFamily
                                    )
                                }
                            }
                        }
                        SettingsItem(GroupPosition.BOTTOM) {
                            DropdownRow(
                                options = availableFontNames.toMutableList(),
                                position = selectedFontIndex,
                                onItemClicked = onFontSelected,
                                onAddClick = onAddFontClick,
                                onRemoveClick = { onRemoveFontClick(it) })
                        }
                    }
                } else {
                    SettingsItem(GroupPosition.BOTTOM) {
                        MosaicTargetRow(
                            selectedTarget = mosaicTarget,
                            onTargetSelected = onMosaicTargetSelected
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingsItem(GroupPosition.SINGLE) {
                HomeSwitchRow(state = isAppIconHidden, onCheckedChange = { onHideIconToggle(it) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            AnimatedVisibility(
                visible = isEasterEggEnabled,
                // 定义进入动画：从下方滑入并淡入
                enter = slideInVertically { it } + fadeIn(),
                // 定义退出动画：向下方滑出并淡出
                exit = slideOutVertically { it } + fadeOut()
            ) {
                SettingsItem(GroupPosition.TOP) {
                    EasterEggRow(
                        isTooDeep = isTooDeep,
                        onTooDeepStateChanged = onTooDeepStateChanged
                    )
                }
            }
        }

        item {
            SettingsItem(if (isEasterEggEnabled) GroupPosition.BOTTOM else GroupPosition.SINGLE) {
                AboutRow { onEasterEggStateChanged(it) }
            }
        }

        item {
            Spacer(Modifier.height(WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()))
        }
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
    isEasterEggEnabled: Boolean,
    onEasterEggStateChanged: (Boolean) -> Unit,
    isTooDeep: Boolean,
    onTooDeepStateChanged: (Boolean) -> Unit,
    mosaicMode: Int,
    onMosaicModeSelected: (Int) -> Unit,
    mosaicTarget: Int,
    onMosaicTargetSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(
            top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
            end = WindowInsets.safeDrawing.asPaddingValues().calculateEndPadding(
                layoutDirection = LayoutDirection.Ltr
            )
        )
    ) {
        SettingsBottomSheetContent(
            emojiOptions = emojiOptions,
            isEditingEmojiList = isEditingEmojiList,
            fontFamily = fontFamily,
            isAppIconHidden = isAppIconHidden,
            availableFontNames = availableFontNames,
            selectedFontIndex = selectedFontIndex,
            onEditClick = onEditClick,
            onEditConfirm = onEditConfirm,
            onHideIconToggle = onHideIconToggle,
            onFontSelected = onFontSelected,
            onAddFontClick = onAddFontClick,
            onRemoveFontClick = onRemoveFontClick,
            isEasterEggEnabled = isEasterEggEnabled,
            onEasterEggStateChanged = onEasterEggStateChanged,
            isTooDeep = isTooDeep,
            onTooDeepStateChanged = onTooDeepStateChanged,
            mosaicMode = mosaicMode,
            onMosaicModeSelected = onMosaicModeSelected,
            mosaicTarget = mosaicTarget,
            onMosaicTargetSelected = onMosaicTargetSelected
        )

        Spacer(Modifier.height(WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()))
    }
}

@Composable
fun EditBottomSheetContent(
    // --- 泛化后的新参数 ---
    mosaicMode: Int,
    // 直接传入瞬时状态对象
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?,
    // Emoji 模式专属参数
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    // 回调
    onEmojiChange: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSlidingStateChange: (Boolean) -> Unit
){
    val (currentAngle, currentSizeFactor) = if (mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI && editingEmoji != null) {
        val factor = if (editingEmoji.originalDiameter > 0f) {
            editingEmoji.diameter / editingEmoji.originalDiameter
        } else { 1.0f }
        editingEmoji.angle to factor
    } else if (mosaicMode == PreferenceRepository.MOSAIC_MODE_BLUR && editingBlurRegion != null) {
        val currentWidth = editingBlurRegion.rect.width()
        val originalWidth = editingBlurRegion.originalRect.width()
        val factor = if (originalWidth > 0f) currentWidth / originalWidth else 1.0f
        editingBlurRegion.angle to factor
    } else {
        0f to 1.0f // 安全回退
    }

    val sizeInteractionSource = remember { MutableInteractionSource() }
    val angleInteractionSource = remember { MutableInteractionSource() }

    val isSizeSliderDragged by sizeInteractionSource.collectIsDraggedAsState()
    val isAngleSliderDragged by angleInteractionSource.collectIsDraggedAsState()

    LaunchedEffect(isSizeSliderDragged, isAngleSliderDragged) {
        onSlidingStateChange(isSizeSliderDragged || isAngleSliderDragged)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        // --- 条件UI：只在 Emoji 模式下显示 ---
        if (mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    modifier = Modifier.width(96.dp),
                    value = editingEmoji?.emoji ?: "",
                    onValueChange = { onEmojiChange(it) }, // 使用新的独立回调
                    label = { Text(stringResource(R.string.new_emoji)) },
                    textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 预置 emoji 选择行
                LazyRow(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(vertical = 8.dp)
                ) {
                    item { Spacer(modifier = Modifier.width(8.dp)) }
                    itemsIndexed(availableEmojis) { _, emoji -> // 使用 items 简化
                        EmojiCardSmall(
                            emoji = emoji,
                            onClick = { onEmojiChange(emoji) }, // 使用新的独立回调
                            fontFamily = fontFamily
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- 通用UI：大小和角度滑块 ---
        Row(modifier = Modifier.fillMaxWidth()) {
            // 大小滑块
            SliderWithCaption(
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = stringResource(R.string.emoji_size), // 文本可以保持不变
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_size),
                value = currentSizeFactor, // 绑定到比例因子
                onValueChange = onSizeChange, // 使用新的独立回调
                minRange = 0.5f,  // 设置合理的比例范围，例如 50%
                maxRange = 2.5f,  // 到 200%
                interactionSource = sizeInteractionSource
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 角度滑块
            SliderWithCaption(
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Rotate90DegreesCw,
                        contentDescription = stringResource(R.string.emoji_angle),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_angle),
                value = currentAngle,
                onValueChange = { onAngleChange(it) }, // 使用新的独立回调
                minRange = -90f,
                maxRange = 90f,
                interactionSource = angleInteractionSource
            )
        }

        // --- 通用UI：确认和取消按钮 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        }
    }
}

@Composable
fun EditSideSheetContent(
    mosaicMode: Int,
    modifier: Modifier = Modifier,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?,
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    onEmojiChange: (String) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSlidingStateChange: (Boolean) -> Unit
) {
    // --- 内部计算：根据模式获取滑块的实时值 ---
    val (currentAngle, currentSizeFactor) = if (mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI && editingEmoji != null) {
        val factor = if (editingEmoji.originalDiameter > 0f) {
            editingEmoji.diameter / editingEmoji.originalDiameter
        } else { 1.0f }
        editingEmoji.angle to factor
    } else if (mosaicMode == PreferenceRepository.MOSAIC_MODE_BLUR && editingBlurRegion != null) {
        val currentWidth = editingBlurRegion.rect.width()
        val originalWidth = editingBlurRegion.originalRect.width()
        val factor = if (originalWidth > 0f) currentWidth / originalWidth else 1.0f
        editingBlurRegion.angle to factor
    } else {
        0f to 1.0f // 安全回退
    }

    val sizeInteractionSource = remember { MutableInteractionSource() }
    val angleInteractionSource = remember { MutableInteractionSource() }

    val isSizeSliderDragged by sizeInteractionSource.collectIsDraggedAsState()
    val isAngleSliderDragged by angleInteractionSource.collectIsDraggedAsState()

    LaunchedEffect(isSizeSliderDragged, isAngleSliderDragged) {
        onSlidingStateChange(isSizeSliderDragged || isAngleSliderDragged)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                start = 16.dp,
                end = WindowInsets.safeDrawing.asPaddingValues().calculateEndPadding(
                    layoutDirection = LayoutDirection.Ltr
                ) + 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // 为所有 item 添加统一间距
    ) {
        // --- 通用UI：确认和取消按钮 ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        }

        // --- 条件UI：只在 Emoji 模式下显示 ---
        if (mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI) {
            item {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editingEmoji?.emoji ?: "",
                    onValueChange = onEmojiChange, // 使用新的独立回调
                    label = { Text(stringResource(R.string.new_emoji)) },
                    textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
                )
            }

            item {
                // 预置 emoji 选择行
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth() // 确保填满宽度
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(vertical = 8.dp)
                ) {
                    item { Spacer(modifier = Modifier.width(8.dp)) }
                    itemsIndexed(availableEmojis) { _, emoji -> // 使用 items 简化
                        EmojiCardSmall(
                            emoji = emoji,
                            onClick = { onEmojiChange(emoji) }, // 使用新的独立回调
                            fontFamily = fontFamily
                        )
                    }
                }
            }
        }

        // --- 通用UI：大小滑块 ---
        item {
            SliderWithCaption(
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = stringResource(R.string.emoji_size),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_size),
                value = currentSizeFactor, // 绑定到实时计算的值
                onValueChange = onSizeChange, // 连接到统一的回调
                minRange = 0.5f,
                maxRange = 2.5f,
                interactionSource = sizeInteractionSource
            )
        }

        // --- 通用UI：角度滑块 ---
        item {
            SliderWithCaption(
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Rotate90DegreesCw,
                        contentDescription = stringResource(R.string.emoji_angle),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                },
                description = stringResource(R.string.emoji_angle),
                value = currentAngle,
                onValueChange = onAngleChange, // 使用新的独立回调
                minRange = -90f,
                maxRange = 90f,
                interactionSource = angleInteractionSource
            )
        }

        item {
            Spacer(
                Modifier.height(
                    WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
fun SideSheetContent(
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?, // 新增参数以接收正在编辑的模糊区域
    showSettingsSheet: Boolean,
) {
    // 首先判断当前是否处于任一编辑模式
    val isEditing = editingEmoji != null || editingBlurRegion != null

    if (isEditing) {
        // --- 调用重构后的通用编辑组件 ---
        EditSideSheetContent(
            mosaicMode = state.mosaicMode,
            // 传递瞬时状态
            editingEmoji = editingEmoji,
            editingBlurRegion = editingBlurRegion,
            // 传递其他参数
            availableEmojis = state.predefinedEmojiList ?: emptyList(),
            fontFamily = state.fontFamily,
            // 统一的回调
            onEmojiChange = actions.onEmojiChange,
            onSizeChange = { newFactor ->
                if (state.mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI) {
                    actions.onSizeFactorChange(newFactor)
                } else {
                    actions.onBlurRegionSizeChange(newFactor)
                }
            },
            onAngleChange = actions.onAngleChange,
            onConfirm = actions.onConfirmEditing,
            onDismiss = actions.onCancelEditing,
            onSlidingStateChange = actions.onSlidingStateChange
        )

    } else if (showSettingsSheet) {
        // --- 这部分逻辑保持不变，用于显示设置面板 ---
        var isEditingEmojiListInSheet by remember { mutableStateOf(false) }
        SettingsSideSheetContent(
            emojiOptions = state.predefinedEmojiList ?: emptyList(),
            isEditingEmojiList = isEditingEmojiListInSheet,
            fontFamily = state.fontFamily,
            isAppIconHidden = state.isAppIconHidden,
            availableFontNames = state.availableFontNames ?: emptyList(),
            selectedFontIndex = state.selectedFontIndex,
            onEditClick = { isEditingEmojiListInSheet = true },
            onEditConfirm = { newEmojiList ->
                actions.onPredefinedEmojisEdited(newEmojiList)
                isEditingEmojiListInSheet = false
            },
            onHideIconToggle = actions.onHideIconToggle,
            onFontSelected = actions.onFontSelected,
            onAddFontClick = actions.onAddFontClick,
            onRemoveFontClick = actions.onRemoveFontClick,
            isEasterEggEnabled = state.isEasterEggEnabled,
            isTooDeep = state.isTooDeep,
            onTooDeepStateChanged = actions.onTooDeepStateChanged,
            onEasterEggStateChanged = actions.onEasterEggStateChanged,
            mosaicMode = state.mosaicMode,
            onMosaicModeSelected = actions.onMosaicModeSelected,
            mosaicTarget = state.mosaicTarget,
            onMosaicTargetSelected = actions.onMosaicTargetSelected
        )
    }
}