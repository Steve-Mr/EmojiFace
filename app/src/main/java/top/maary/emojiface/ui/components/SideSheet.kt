package top.maary.emojiface.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState

/**
 * 一个通用的 SideSheet 容器，封装了打开/关闭状态管理和内容显示。
 *
 * @param showSheet 是否请求显示 SideSheet。
 * @param onDismissSheet 当 SideSheet 关闭时的回调。
 * @param sheetContent SideSheet 中要显示的内容。
 * @param content 主屏幕内容。
 */
@Composable
fun SideSheet(
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    isModal: Boolean = true,
    sheetContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 同步外部请求状态与抽屉的打开/关闭状态
    LaunchedEffect(showSheet) {
        if (showSheet) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // 监听抽屉的物理关闭事件（手势、点击外部），并触发回调
    LaunchedEffect(drawerState.currentValue) {
        if (isModal && drawerState.currentValue == DrawerValue.Closed && showSheet) {
            onDismissSheet()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isModal && showSheet,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        // 直接渲染传入的 sheetContent
                        sheetContent()
                    }
                }
            }
        ) {
            // 恢复主内容的布局方向
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                content()
            }
        }
    }
}

@Composable
fun SideSheetContent(
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    showSettingsSheet: Boolean
) {
    val scrollState = rememberScrollState()
        if (editingEmoji != null) {
            val maxDiameter = state.currentImage?.let { minOf(it.width, it.height) / 3f } ?: 500f
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
                onRemoveFontClick = actions.onRemoveFontClick
            )
        }

}