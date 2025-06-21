package top.maary.emojiface.ui.edit // 或者你選擇的包名

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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.maary.emojiface.R
import top.maary.emojiface.ui.components.ActionRow
import top.maary.emojiface.ui.components.DisplayPane
import top.maary.emojiface.ui.components.EditEmojiBottomSheetContent
import top.maary.emojiface.ui.components.EditEmojiSideSheetContent
import top.maary.emojiface.ui.components.EmojiCard
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactScreenLayout(state: EditScreenState, actions: EditScreenActions, editingEmoji: EmojiDetection?) {
    // TopAppBar 滾動行為
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.shadow(8.dp), // 保留陰影
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.tertiary,
                ),
                title = {
                    // Compact 版本中標題為空
                },
                navigationIcon = {
                    IconButton(onClick = actions.onCloseClick) { // 使用 action
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.exit)
                        )
                    }
                },
                actions = {
                    // 只有在有圖片加載/處理後才顯示清除按鈕
                    if (state.displayedBitmap != null) {
                        IconButton(onClick = actions.onClearImageClick) { // 使用 action
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
                modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                state = state,
                actions = actions,
                editingEmoji = editingEmoji
            )

            if (state.displayedBitmap != null) {
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
                        itemsIndexed(state.emojiDetections) { index, detection ->
                            Spacer(modifier = Modifier.width(8.dp))
                            EmojiCard(
                                emoji = detection.emoji,
                                onClick = { actions.onEmojiCardClick(index) }, // 使用 action
                                fontFamily = state.fontFamily,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                // hPadding 和 vPadding 使用 EmojiCard 的默認值或按需調整
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.width(8.dp))
                            EmojiCard(
                                emoji = "➕", // 或使用 Icons.Outlined.AddReaction
                                onClick = actions.onAddEmojiCardClick, // 使用 action
                                clickable = true,
                                fontFamily = state.fontFamily,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                    }
                }
            }

            // --- 底部操作按鈕區域 ---
            ActionRow(state = state, actions = actions)

            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
//            }

        }
    }
    if (editingEmoji != null) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val maxDiameter = state.currentImage?.let { minOf(it.width, it.height) / 3f } ?: 500f

        ModalBottomSheet(
            onDismissRequest = actions.onCancelEditing,
            sheetState = bottomSheetState,
            dragHandle = { },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f)
        ) {
            // 完全复用现有的 Composable
            EditEmojiBottomSheetContent(
                initialEmoji = editingEmoji.emoji,
                initialDiameter = editingEmoji.diameter,
                initialRotation = editingEmoji.angle,
                availableEmojis = state.predefinedEmojiList ?: emptyList(),
                fontFamily = state.fontFamily,
                onConfirm = actions.onConfirmEditing,
                onDismiss = actions.onCancelEditing,
                maxDiameter = maxDiameter,
                onValueChange = actions.onEditingValueChanged
            )
        }
    }
}

@Composable
fun LargeScreenLayout(state: EditScreenState, actions: EditScreenActions, editingEmoji: EmojiDetection?) {
//    Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer // Consistent background
//    ) { innerPadding -> // Scaffold provides padding, respect it if needed, though NavSuite might handle it
//        NavigationSuiteScaffold(
//            navigationSuiteItems = {
//                // --- Navigation Rail Items ---
//                item(
//                    icon = {
//                        Icon(
//                            imageVector = Icons.Outlined.Close,
//                            contentDescription = stringResource(R.string.exit)
//                        )
//                    },
//                    label = { Text(stringResource(R.string.exit)) }, // Show label in NavRail
//                    selected = false, // Never selected state
//                    onClick = actions.onCloseClick // Use action
//                )
//                // Show clear button only if an image is loaded
//                if (state.displayedBitmap != null) {
//                    item(
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Outlined.DeleteSweep,
//                                contentDescription = stringResource(R.string.clear_photo)
//                            )
//                        },
//                        // label = { Text("Clear") }, // Optional label
//                        selected = false,
//                        onClick = actions.onClearImageClick // Use action
//                    )
//                }
//            },
//            layoutType = NavigationSuiteType.NavigationRail, // Explicitly set type
//            navigationSuiteColors = NavigationSuiteDefaults.colors( // Consistent colors
//                navigationRailContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
//                navigationRailContentColor = MaterialTheme.colorScheme.tertiary,
//            )
//        ) {
//            // --- Main Content Area (beside NavRail) ---
//            Row(
//                modifier = Modifier
//                    .fillMaxSize().padding(innerPadding) // Check if needed depending on NavSuiteScaffold behavior
//            ) {
//                // --- Image Display Area (Larger Portion) ---
//                DisplayPane(modifier = Modifier.weight(2f).fillMaxHeight(), state = state, actions = actions, editingEmoji = editingEmoji)
//
//                // --- Side Panel (Smaller Portion) ---
//                Card(
//                    modifier = Modifier
//                        .weight(1f) // Takes 1/3 of the width
//                        .fillMaxHeight()
//                        .padding(end = 8.dp), // Padding around card
//                    colors = CardDefaults.cardColors( // Consistent card color
//                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.fillMaxHeight(), // Column fills the card height
//                        verticalArrangement = Arrangement.SpaceBetween // Pushes grid up and buttons down
//                    ) {
//                        // --- Emoji Grid ---
//                        LazyVerticalGrid(
//                            columns = GridCells.Adaptive(minSize = 76.dp), // Adaptive columns
//                            modifier = Modifier.weight(1f) // Grid takes available space
//                        ) {
//                            itemsIndexed(state.emojiDetections) { index, detection ->
//                                EmojiCard(
//                                    emoji = detection.emoji,
//                                    onClick = { actions.onEmojiCardClick(index) }, // Use action
//                                    fontFamily = state.fontFamily,
//                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
//                                    hPadding = 8.dp,
//                                    vPadding = 8.dp
//                                )
//                            }
//                            // Show Add button only if an image is present
//                            if (state.displayedBitmap != null) {
//                                item {
//                                    EmojiCard(
//                                        emoji = "➕",
//                                        onClick = actions.onAddEmojiCardClick, // Use action
//                                        clickable = true,
//                                        fontFamily = state.fontFamily,
//                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
//                                        hPadding = 8.dp,
//                                        vPadding = 8.dp
//                                    )
//                                }
//                            }
//                        }
//
//                        // --- Action Buttons Area (at the bottom of the card) ---
//                        ActionRow(state = state, actions = actions)
//                    }
//                }
//            }
//        }
//    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 2. 使用 LaunchedEffect 同步 ViewModel 状态 和 SideSheet 状态
    LaunchedEffect(editingEmoji) {
        if (editingEmoji != null) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // 3. 监听 SideSheet 的关闭事件 (比如点击外部)，并通知 ViewModel
    LaunchedEffect(drawerState.currentValue) {
        // 如果用户关闭了抽屉，但 ViewModel 的状态仍然是“正在编辑”，则调用取消操作
        if (drawerState.currentValue == DrawerValue.Closed && editingEmoji != null) {
            actions.onCancelEditing()
        }
    }

    // 4. 使用 CompositionLocalProvider 将抽屉强制设置为从右向左 (RTL) 弹出
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = editingEmoji != null, // 仅在编辑时允许手势
            drawerContent = {
                // 在抽屉内容内部，将布局方向恢复为从左到右 (LTR)，以免影响内容本身
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(360.dp) // 设置 SideSheet 的宽度
                    ) {
                        if (editingEmoji != null) {
                            val maxDiameter = state.currentImage?.let { minOf(it.width, it.height) / 3f } ?: 500f
                            // 再次复用完全相同的 EditEmojiBottomSheetContent
                            EditEmojiSideSheetContent(
                                initialEmoji = editingEmoji.emoji,
                                initialDiameter = editingEmoji.diameter,
                                initialRotation = editingEmoji.angle,
                                availableEmojis = state.predefinedEmojiList ?: emptyList(),
                                fontFamily = state.fontFamily,
                                onConfirm = actions.onConfirmEditing,
                                onDismiss = actions.onCancelEditing,
                                maxDiameter = maxDiameter,
                                onValueChange = actions.onEditingValueChanged
                            )
                        }
                    }
                }
            }
        ) {
            // 5. 将原有的 Scaffold 和 NavigationSuiteScaffold 作为主内容
            // 同样，恢复 LTR 方向以确保主屏幕布局正常
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(containerColor = MaterialTheme.colorScheme.surfaceContainer // Consistent background
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
                        navigationSuiteColors = NavigationSuiteDefaults.colors( // Consistent colors
                            navigationRailContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            navigationRailContentColor = MaterialTheme.colorScheme.tertiary,
                        )
                    ) {
                        // --- Main Content Area (beside NavRail) ---
                        Row(
                            modifier = Modifier
                                .fillMaxSize().padding(innerPadding) // Check if needed depending on NavSuiteScaffold behavior
                        ) {
                            // --- Image Display Area (Larger Portion) ---
                            DisplayPane(modifier = Modifier.weight(2f).fillMaxHeight(), state = state, actions = actions, editingEmoji = editingEmoji)

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
                                    // --- Emoji Grid ---
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 76.dp), // Adaptive columns
                                        modifier = Modifier.weight(1f) // Grid takes available space
                                    ) {
                                        itemsIndexed(state.emojiDetections) { index, detection ->
                                            EmojiCard(
                                                emoji = detection.emoji,
                                                onClick = { actions.onEmojiCardClick(index) }, // Use action
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
                                                    onClick = actions.onAddEmojiCardClick, // Use action
                                                    clickable = true,
                                                    fontFamily = state.fontFamily,
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                    hPadding = 8.dp,
                                                    vPadding = 8.dp
                                                )
                                            }
                                        }
                                    }

                                    // --- Action Buttons Area (at the bottom of the card) ---
                                    ActionRow(state = state, actions = actions)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}