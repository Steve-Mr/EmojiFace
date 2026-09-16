package top.maary.emojiface.ui.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.maary.emojiface.R
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.domain.model.Avatar
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
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
    onMosaicTargetSelected: (Int) -> Unit,
    onManageAvatarsClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.animateContentSize()
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
                    val exitTransition = slideOutVertically { height -> height } + fadeOut(
                        animationSpec = tween(300)
                    )
                    val enterTransition = slideInVertically { height -> height } + fadeIn(
                        animationSpec = tween(300, delayMillis = 300)
                    )
                    (enterTransition togetherWith exitTransition).using(
                        SizeTransform(clip = true)
                    )
                }
            ) { targetMode ->
                when (targetMode) {
                    PreferenceRepository.MOSAIC_MODE_EMOJI -> {
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
                    }
                    else -> {
                        SettingsItem(GroupPosition.BOTTOM) {
                            MosaicTargetRow(
                                selectedTarget = mosaicTarget,
                                onTargetSelected = onMosaicTargetSelected
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SettingsItem(GroupPosition.SINGLE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onManageAvatarsClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.manage_avatars),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.manage_avatars),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                enter = slideInVertically { it } + fadeIn(),
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
    onMosaicTargetSelected: (Int) -> Unit,
    onManageAvatarsClick: () -> Unit = {}
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
            onMosaicTargetSelected = onMosaicTargetSelected,
            onManageAvatarsClick = onManageAvatarsClick
        )

        Spacer(Modifier.height(WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEditBottomSheetContent(
    editingOcclusion: FaceOcclusion,
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    availableAvatars: List<Avatar>,
    cachedAvatarBitmaps: Map<String, Bitmap>,
    onModeChange: (OcclusionMode) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onManageAvatarsClick: () -> Unit,
    onSlidingStateChange: (Boolean) -> Unit
) {
    val currentModeIndex = when (editingOcclusion.mode) {
        is OcclusionMode.Emoji -> 0
        is OcclusionMode.Blur -> 1
        is OcclusionMode.Avatar -> 2
    }

    val currentScale = editingOcclusion.geometry.scaleFactor
    val currentAngle = editingOcclusion.geometry.rotationDegrees

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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            val modes = listOf(
                stringResource(R.string.emoji),
                stringResource(R.string.mosaic),
                stringResource(R.string.avatar)
            )
            modes.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    onClick = {
                        if (index != currentModeIndex) {
                            when (index) {
                                0 -> onModeChange(
                                    OcclusionMode.Emoji(
                                        emoji = availableEmojis.firstOrNull() ?: "😂"
                                    )
                                )
                                1 -> onModeChange(
                                    OcclusionMode.Blur(
                                        type = OcclusionMode.Blur.BlurType.Gaussian,
                                        target = OcclusionMode.Blur.BlurTarget.Face
                                    )
                                )
                                2 -> {
                                    val activeId = availableAvatars.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                                    onModeChange(
                                        OcclusionMode.Avatar(
                                            avatarId = activeId,
                                            cropShape = OcclusionMode.Avatar.AvatarCropShape.Circle
                                        )
                                    )
                                }
                            }
                        }
                    },
                    selected = currentModeIndex == index
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = currentModeIndex,
            label = "EditModeSpecificUI"
        ) { modeIndex ->
            when (modeIndex) {
                0 -> {
                    val emojiMode = editingOcclusion.mode as? OcclusionMode.Emoji ?: OcclusionMode.Emoji("😂")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.width(96.dp),
                            value = emojiMode.emoji,
                            onValueChange = { onModeChange(emojiMode.copy(emoji = it)) },
                            label = { Text(stringResource(R.string.new_emoji)) },
                            textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(vertical = 8.dp)
                        ) {
                            item { Spacer(modifier = Modifier.width(8.dp)) }
                            itemsIndexed(availableEmojis) { _, emoji ->
                                EmojiCardSmall(
                                    emoji = emoji,
                                    onClick = { onModeChange(emojiMode.copy(emoji = emoji)) },
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
                1 -> {
                    val blurMode = editingOcclusion.mode as? OcclusionMode.Blur ?: OcclusionMode.Blur()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val styles = listOf(
                                OcclusionMode.Blur.BlurType.Gaussian to stringResource(R.string.gaussian),
                                OcclusionMode.Blur.BlurType.Pixelated to stringResource(R.string.pixelated),
                                OcclusionMode.Blur.BlurType.Halftone to stringResource(R.string.halftone)
                            )
                            styles.forEachIndexed { index, (type, label) ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                                    onClick = { onModeChange(blurMode.copy(type = type)) },
                                    selected = blurMode.type == type
                                ) {
                                    Text(label)
                                }
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val targets = listOf(
                                OcclusionMode.Blur.BlurTarget.Face to stringResource(R.string.face),
                                OcclusionMode.Blur.BlurTarget.Eyes to stringResource(R.string.eyes)
                            )
                            targets.forEachIndexed { index, (target, label) ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = targets.size),
                                    onClick = { onModeChange(blurMode.copy(target = target)) },
                                    selected = blurMode.target == target
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    val avatarMode = editingOcclusion.mode as? OcclusionMode.Avatar ?: OcclusionMode.Avatar(
                        avatarId = availableAvatars.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clickable { onManageAvatarsClick() },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.import_avatar))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            if (availableAvatars.isEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.no_avatars_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(availableAvatars) { _, avatar ->
                                    val isSelected = avatarMode.avatarId == avatar.id
                                    val bitmap = cachedAvatarBitmaps[avatar.id]
                                    Card(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .padding(end = 8.dp)
                                            .clickable { onModeChange(avatarMode.copy(avatarId = avatar.id)) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = avatar.displayName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.AccountCircle, contentDescription = avatar.displayName)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val shapes = listOf(
                                OcclusionMode.Avatar.AvatarCropShape.Circle to stringResource(R.string.circle),
                                OcclusionMode.Avatar.AvatarCropShape.RoundedRect to stringResource(R.string.rounded_rect),
                                OcclusionMode.Avatar.AvatarCropShape.Square to stringResource(R.string.square)
                            )
                            shapes.forEachIndexed { index, (shape, label) ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = shapes.size),
                                    onClick = { onModeChange(avatarMode.copy(cropShape = shape)) },
                                    selected = avatarMode.cropShape == shape
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            SliderWithCaption(
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FormatSize,
                        contentDescription = stringResource(R.string.emoji_size),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp).size(16.dp)
                    )
                },
                description = "${stringResource(R.string.emoji_size)}: ${"%.2f".format(currentScale)}x",
                value = currentScale,
                onValueChange = onSizeChange,
                minRange = 0.5f,
                maxRange = 2.5f,
                interactionSource = sizeInteractionSource
            )

            Spacer(modifier = Modifier.width(8.dp))

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
                description = "${stringResource(R.string.emoji_angle)}: ${currentAngle.toInt()}°",
                value = currentAngle,
                onValueChange = onAngleChange,
                minRange = -180f,
                maxRange = 180f,
                interactionSource = angleInteractionSource
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_face),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.delete_face))
            }

            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEditSideSheetContent(
    editingOcclusion: FaceOcclusion,
    availableEmojis: List<String>,
    fontFamily: FontFamily?,
    availableAvatars: List<Avatar>,
    cachedAvatarBitmaps: Map<String, Bitmap>,
    onModeChange: (OcclusionMode) -> Unit,
    onSizeChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onManageAvatarsClick: () -> Unit,
    onSlidingStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentModeIndex = when (editingOcclusion.mode) {
        is OcclusionMode.Emoji -> 0
        is OcclusionMode.Blur -> 1
        is OcclusionMode.Avatar -> 2
    }

    val currentScale = editingOcclusion.geometry.scaleFactor
    val currentAngle = editingOcclusion.geometry.rotationDegrees

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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_face),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete_face))
                }

                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text(stringResource(R.string.ok))
                    }
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val modes = listOf(
                    stringResource(R.string.emoji),
                    stringResource(R.string.mosaic),
                    stringResource(R.string.avatar)
                )
                modes.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        onClick = {
                            if (index != currentModeIndex) {
                                when (index) {
                                    0 -> onModeChange(
                                        OcclusionMode.Emoji(
                                            emoji = availableEmojis.firstOrNull() ?: "😂"
                                        )
                                    )
                                    1 -> onModeChange(
                                        OcclusionMode.Blur(
                                            type = OcclusionMode.Blur.BlurType.Gaussian,
                                            target = OcclusionMode.Blur.BlurTarget.Face
                                        )
                                    )
                                    2 -> {
                                        val activeId = availableAvatars.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                                        onModeChange(
                                            OcclusionMode.Avatar(
                                                avatarId = activeId,
                                                cropShape = OcclusionMode.Avatar.AvatarCropShape.Circle
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        selected = currentModeIndex == index
                    ) {
                        Text(label)
                    }
                }
            }
        }

        when (currentModeIndex) {
            0 -> {
                val emojiMode = editingOcclusion.mode as? OcclusionMode.Emoji ?: OcclusionMode.Emoji("😂")
                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = emojiMode.emoji,
                        onValueChange = { onModeChange(emojiMode.copy(emoji = it)) },
                        label = { Text(stringResource(R.string.new_emoji)) },
                        textStyle = TextStyle(fontFamily = fontFamily, fontSize = 20.sp)
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(vertical = 8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.width(8.dp)) }
                        itemsIndexed(availableEmojis) { _, emoji ->
                            EmojiCardSmall(
                                emoji = emoji,
                                onClick = { onModeChange(emojiMode.copy(emoji = emoji)) },
                                fontFamily = fontFamily
                            )
                        }
                    }
                }
            }
            1 -> {
                val blurMode = editingOcclusion.mode as? OcclusionMode.Blur ?: OcclusionMode.Blur()
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val styles = listOf(
                            OcclusionMode.Blur.BlurType.Gaussian to stringResource(R.string.gaussian),
                            OcclusionMode.Blur.BlurType.Pixelated to stringResource(R.string.pixelated),
                            OcclusionMode.Blur.BlurType.Halftone to stringResource(R.string.halftone)
                        )
                        styles.forEachIndexed { index, (type, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                                onClick = { onModeChange(blurMode.copy(type = type)) },
                                selected = blurMode.type == type
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val targets = listOf(
                            OcclusionMode.Blur.BlurTarget.Face to stringResource(R.string.face),
                            OcclusionMode.Blur.BlurTarget.Eyes to stringResource(R.string.eyes)
                        )
                        targets.forEachIndexed { index, (target, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = targets.size),
                                onClick = { onModeChange(blurMode.copy(target = target)) },
                                selected = blurMode.target == target
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
            2 -> {
                val avatarMode = editingOcclusion.mode as? OcclusionMode.Avatar ?: OcclusionMode.Avatar(
                    avatarId = availableAvatars.firstOrNull()?.id ?: PreferenceRepository.DEFAULT_AVATAR_ID
                )
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { onManageAvatarsClick() },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.import_avatar))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (availableAvatars.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_avatars_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(availableAvatars) { _, avatar ->
                                val isSelected = avatarMode.avatarId == avatar.id
                                val bitmap = cachedAvatarBitmaps[avatar.id]
                                Card(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(end = 8.dp)
                                        .clickable { onModeChange(avatarMode.copy(avatarId = avatar.id)) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = avatar.displayName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.AccountCircle, contentDescription = avatar.displayName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val shapes = listOf(
                            OcclusionMode.Avatar.AvatarCropShape.Circle to stringResource(R.string.circle),
                            OcclusionMode.Avatar.AvatarCropShape.RoundedRect to stringResource(R.string.rounded_rect),
                            OcclusionMode.Avatar.AvatarCropShape.Square to stringResource(R.string.square)
                        )
                        shapes.forEachIndexed { index, (shape, label) ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = shapes.size),
                                onClick = { onModeChange(avatarMode.copy(cropShape = shape)) },
                                selected = avatarMode.cropShape == shape
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

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
                description = "${stringResource(R.string.emoji_size)}: ${"%.2f".format(currentScale)}x",
                value = currentScale,
                onValueChange = onSizeChange,
                minRange = 0.5f,
                maxRange = 2.5f,
                interactionSource = sizeInteractionSource
            )
        }

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
                description = "${stringResource(R.string.emoji_angle)}: ${currentAngle.toInt()}°",
                value = currentAngle,
                onValueChange = onAngleChange,
                minRange = -180f,
                maxRange = 180f,
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
    showSettingsSheet: Boolean,
    editingOcclusion: FaceOcclusion? = state.editingOcclusion
) {
    if (editingOcclusion != null) {
        UnifiedEditSideSheetContent(
            editingOcclusion = editingOcclusion,
            availableEmojis = state.predefinedEmojiList ?: emptyList(),
            fontFamily = state.fontFamily,
            availableAvatars = state.availableAvatars,
            cachedAvatarBitmaps = state.cachedAvatarBitmaps,
            onModeChange = actions.onOcclusionModeChange,
            onSizeChange = actions.onOcclusionSizeChange,
            onAngleChange = actions.onOcclusionAngleChange,
            onConfirm = actions.onConfirmEditing,
            onDismiss = actions.onCancelEditing,
            onDelete = {
                val index = state.editingOcclusionIndex
                if (index != null && index >= 0) {
                    actions.onDeleteOcclusion(index)
                } else {
                    actions.onCancelEditing()
                }
            },
            onManageAvatarsClick = actions.onManageAvatarsClick,
            onSlidingStateChange = actions.onSlidingStateChange
        )
    } else if (showSettingsSheet) {
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
            onMosaicTargetSelected = actions.onMosaicTargetSelected,
            onManageAvatarsClick = actions.onManageAvatarsClick
        )
    }
}