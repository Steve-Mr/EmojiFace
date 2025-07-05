package top.maary.emojiface.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 一个通用的 SideSheet 容器，封装了打开/关闭状态管理和内容显示。
 *
 * @param showSheet 是否请求显示 SideSheet。
 * @param onDismissSheet 当 SideSheet 关闭时的回调。
 * @param sheetContent SideSheet 中要显示的内容。
 * @param content 主屏幕内容。
 */
@Composable
fun DrawerSideSheet(
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
                        windowInsets = WindowInsets(0.dp),
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

