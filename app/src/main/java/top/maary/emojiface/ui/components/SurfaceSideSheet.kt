package top.maary.emojiface.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun SurfaceSideSheet(
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    isModal: Boolean = true,
    sheetContainerColor: Color = MaterialTheme.colorScheme.surface,
    sheetContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    // 状态来驱动动画
    var animationState by remember { mutableStateOf(showSheet) }
    LaunchedEffect(showSheet) {
        animationState = showSheet
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 主屏幕内容
        content()

        // 2. 遮罩层 (Scrim)
        val scrimColor by animateColorAsState(
            targetValue = if (animationState) Color.Black.copy(alpha = 0.32f) else Color.Transparent,
            animationSpec = tween(),
            label = "scrimColor"
        )

        if (scrimColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    // 仅在 isModal 为 true 时，才允许通过点击关闭
                    .then(if (isModal) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { onDismissSheet() }
                        }
                    } else {
                        Modifier
                    })
            )
        }


        // 3. 抽屉面板内容
        val density = LocalDensity.current
        val sheetWidth = 320.dp

        val offset by animateIntOffsetAsState(
            targetValue = if (animationState) {
                IntOffset.Zero
            } else {
                IntOffset(with(density) { sheetWidth.toPx() }.toInt(), 0)
            },
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "sheetOffset"
        )

        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(sheetWidth)
                .align(Alignment.CenterEnd)
                .offset { offset },
            color = sheetContainerColor
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                sheetContent()
            }
        }
    }
}