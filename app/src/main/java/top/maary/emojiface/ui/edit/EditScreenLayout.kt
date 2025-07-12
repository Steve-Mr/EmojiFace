package top.maary.emojiface.ui.edit // 或者你選擇的包名

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.maary.emojiface.R
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_BLUR
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_MODE_EMOJI
import top.maary.emojiface.ui.components.ActionRow
import top.maary.emojiface.ui.components.DisplayPane
import top.maary.emojiface.ui.components.EditBottomSheetContent
import top.maary.emojiface.ui.components.EmojiCard
import top.maary.emojiface.ui.components.MosaicTypeBlock
import top.maary.emojiface.ui.components.MosaicTypeToolbar
import top.maary.emojiface.ui.components.SettingsBottomSheetContent
import top.maary.emojiface.ui.components.SideSheetContent
import top.maary.emojiface.ui.components.SurfaceSideSheet
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactScreenLayout(
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?,
    showSettingsSheet: Boolean,
    onDismissSettingsSheet: () -> Unit,
    isMediumLayout: Boolean = false
) {
    // TopAppBar 滾動行為
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.shadow(8.dp), // 保留陰影
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.tertiary),
                title = {
                    Text(stringResource(R.string.app_name))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onCloseClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary)) { // 使用 action
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.exit)
                        )
                    }
                },
                actions = {
                    // 只有在有圖片加載/處理後才顯示清除按鈕
                    if (state.displayedBitmap != null) {
                        IconButton(onClick = actions.onClearImageClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.secondary)) { // 使用 action
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest // 背景色
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = innerPadding.calculateTopPadding()) // 應用 Scaffold 的內邊距
        ) {
            // --- 圖片顯示區域 ---
            DisplayPane(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                state = state,
                actions = actions,
                editingEmoji = editingEmoji,
                editingBlurRegion = editingBlurRegion
            )

            if (state.displayedBitmap != null) {
                if (state.mosaicMode == MOSAIC_MODE_EMOJI) {
                    Card(
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        // --- 偵測到的表情符號行 ---
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(
                                horizontal = 0.dp,
                                vertical = 8.dp
                            ) // 增加垂直 padding
                        ) {
                            itemsIndexed(
                                items = state.emojiDetections,
                                key = { _, item -> item.id } // key 使用 item 的唯一 id
                            ) { index, detection ->
                                Spacer(modifier = Modifier.width(8.dp))
                                EmojiCard(
                                    modifier = Modifier.animateItem(),
                                    emoji = detection.emoji,
                                    onClick = { actions.onEmojiCardClick(index) },
                                    onLongClick = { actions.onEmojiCardLongClick(index) },
                                    fontFamily = state.fontFamily,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    // hPadding 和 vPadding 使用 EmojiCard 的默認值或按需調整
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.width(8.dp))
                                EmojiCard(
                                    emoji = "➕", // 或使用 Icons.Outlined.AddReaction
                                    onClick = actions.onAddClicked, // 使用 action
                                    clickable = true,
                                    fontFamily = state.fontFamily,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                        }
                    }
                } else {
                    MosaicTypeToolbar(
                        selectedType = state.mosaicType, // <-- 传递当前选中的类型
                        onMosaicTypeSelected = actions.onMosaicTypeSelected, // <-- 传递 Action
                        onAddBlurRegionClick = actions.onAddClicked
                    )
                }
            }

            // --- 底部操作按鈕區域 ---
            ActionRow(state = state, actions = actions, isMediumLayout = isMediumLayout)

            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))

        }
    }
    if (editingEmoji != null || editingBlurRegion != null) {
        val bottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
        val scope = rememberCoroutineScope()

        var isSliding by remember { mutableStateOf(false) }

        val containerColor by animateColorAsState(
            targetValue = if (isSliding) {
                MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f)
            },
            label = "BottomSheetColorAnimation"
        )

        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    // 重新展开，以阻止关闭
                    bottomSheetState.show()
                }
            },
            sheetState = bottomSheetState,
            dragHandle = { },
            sheetGesturesEnabled = false,
            containerColor = containerColor
        ) {
            // 完全复用现有的 Composable
            EditBottomSheetContent(
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
                    if (state.mosaicMode == MOSAIC_MODE_EMOJI) {
                        actions.onSizeFactorChange(newFactor)
                    } else {
                        actions.onBlurRegionSizeChange(newFactor)
                    }
                },
                onAngleChange = actions.onAngleChange,
                onConfirm = actions.onConfirmEditing,
                onDismiss = actions.onCancelEditing,
                onSlidingStateChange = actions.onSlidingStateChange,
            )
        }
    }
    if (showSettingsSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // 将这个状态管理移到这里，使其生命周期与 Sheet 绑定
        var isEditingEmojiListInSheet by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = onDismissSettingsSheet, // 使用传递进来的 dismiss 函数
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            SettingsBottomSheetContent(
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
                onEasterEggStateChanged = actions.onEasterEggStateChanged,
                isTooDeep = state.isTooDeep,
                onTooDeepStateChanged = actions.onTooDeepStateChanged,
                mosaicMode = state.mosaicMode,
                onMosaicModeSelected = actions.onMosaicModeSelected,
                mosaicTarget = state.mosaicTarget,
                onMosaicTargetSelected = actions.onMosaicTargetSelected
            )
        }
    }
}

@Composable
fun LargeScreenLayout(
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?,
    showSettingsSheet: Boolean,
    onDismissSettingsSheet: () -> Unit,
    isMediumLayout: Boolean
) {
    val showSideSheet = editingEmoji != null || showSettingsSheet || editingBlurRegion != null

    var isSliding by remember { mutableStateOf(false) }

    val sideSheetColor by animateColorAsState(
        targetValue = if (isSliding) {
            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
        } else if (editingEmoji != null){
            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "SideSheetColorAnimation"
    )

    val onDismiss = {
        if (editingEmoji != null) {
            actions.onCancelEditing()
        } else {
            onDismissSettingsSheet()
        }
    }

    SurfaceSideSheet (
        showSheet = showSideSheet,
        onDismissSheet = onDismiss,
        isModal = (editingEmoji == null),
        sheetContainerColor = sideSheetColor,
        sheetContent = {
            SideSheetContent(
                state = state,
                actions = actions,
                editingEmoji = editingEmoji,
                showSettingsSheet = showSettingsSheet,
                editingBlurRegion = editingBlurRegion
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer // Consistent background
        ) { innerPadding -> // Scaffold provides padding, respect it if needed, though NavSuite might handle it
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    // --- Navigation Rail Items ---
                    item(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.exit)
                            )
                        },
                        label = { Text(stringResource(R.string.exit)) }, // Show label in NavRail
                        selected = false, // Never selected state
                        onClick = actions.onCloseClick // Use action
                    )
                    // Show clear button only if an image is loaded
                    if (state.displayedBitmap != null) {
                        item(
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = stringResource(R.string.clear_photo)
                                )
                            },
                            // label = { Text("Clear") }, // Optional label
                            selected = false,
                            onClick = actions.onClearImageClick // Use action
                        )
                    }
                },
                layoutType = NavigationSuiteType.NavigationRail, // Explicitly set type
                navigationSuiteColors = NavigationSuiteDefaults.colors(
                    // Consistent colors
                    navigationRailContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    navigationRailContentColor = MaterialTheme.colorScheme.tertiary,
                )
            ) {
                // --- Main Content Area (beside NavRail) ---
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // Check if needed depending on NavSuiteScaffold behavior
                ) {
                    // --- Image Display Area (Larger Portion) ---
                    DisplayPane(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        state = state,
                        actions = actions,
                        editingEmoji = editingEmoji,
                        editingBlurRegion = editingBlurRegion
                    )

                    // --- Side Panel (Smaller Portion) ---
                    Card(
                        modifier = Modifier
                            .weight(1f) // Takes 1/3 of the width
                            .fillMaxHeight()
                            .padding(end = 8.dp), // Padding around card
                        colors = CardDefaults.cardColors( // Consistent card color
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(), // Column fills the card height
                            verticalArrangement = Arrangement.SpaceBetween // Pushes grid up and buttons down
                        ) {
                            if (state.mosaicMode == MOSAIC_MODE_EMOJI) {
                                // --- Emoji Grid ---
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 76.dp), // Adaptive columns
                                    modifier = Modifier.weight(1f) // Grid takes available space
                                ) {
                                    itemsIndexed(
                                        items = state.emojiDetections,
                                        key = { _, item -> item.id } // key 使用 item 的唯一 id
                                    ) { index, detection ->
                                        EmojiCard(
                                            modifier = Modifier.animateItem(),
                                            emoji = detection.emoji,
                                            onClick = { actions.onEmojiCardClick(index) },
                                            onLongClick = { actions.onEmojiCardLongClick(index) },
                                            fontFamily = state.fontFamily,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            hPadding = 8.dp,
                                            vPadding = 8.dp
                                        )
                                    }
                                    // Show Add button only if an image is present
                                    if (state.displayedBitmap != null) {
                                        item {
                                            EmojiCard(
                                                emoji = "➕",
                                                onClick = actions.onAddClicked, // Use action
                                                clickable = true,
                                                fontFamily = state.fontFamily,
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                hPadding = 8.dp,
                                                vPadding = 8.dp
                                            )
                                        }
                                    }
                                }
                            } else if (state.mosaicMode == MOSAIC_MODE_BLUR) {
                                if (state.displayedBitmap != null) {
                                    MosaicTypeBlock(
                                        selectedType = state.mosaicType, // <-- 传递当前选中的类型
                                        onMosaicTypeSelected = actions.onMosaicTypeSelected, // <-- 传递 Action
                                        onAddBlurRegionClick = actions.onAddClicked
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            // --- Action Buttons Area (at the bottom of the card) ---
                            ActionRow(state = state, actions = actions, isMediumLayout = isMediumLayout)
                        }
                    }
                }
            }
        }
    }
}