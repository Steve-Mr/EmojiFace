package top.maary.emojiface.ui.components

import android.graphics.Color.parseColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    cornersRadius: Dp = 0.dp,
    // 控制 glow 的粗细，其同时也作为 Blur 的参数
    glowingStrokeWidth: Dp = 8.dp,
    ratio: Float,
    animate: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    // 创建无限动画控制 SweepGradient 的旋转角度
    val infiniteTransition = rememberInfiniteTransition(label = "Transition")
    val rotationAngle by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "animateFloat"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }


    Box(
        modifier = modifier
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                if (animate) {

                    // 定义一组渐变颜色（最后一个颜色和第一个保持一致，使渐变闭合）
                    val gradientColors = listOf(
                        Color(parseColor("#fb9de8")),
                        Color(parseColor("#9ea4fd")),
                        Color(parseColor("#fb9de8")),
                        )

                    // 构造旋转矩阵
                    val matrix = android.graphics.Matrix().apply {
                        postRotate(rotationAngle, centerX, centerY)
                    }

                    // 使用原生 SweepGradient 创建环形渐变
                    val shader = android.graphics.SweepGradient(
                        centerX,
                        centerY,
                        gradientColors.map { it.toArgb() }.toIntArray(),
                        null
                    ).apply {
                        setLocalMatrix(matrix)
                    }

                    // 创建 Paint 用于绘制边缘 glow，设置为 STROKE 模式，并添加模糊效果
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = glowingStrokeWidth.toPx()
                        this.shader = shader
                        // 使用 BlurMaskFilter 产生 glow 效果
                        maskFilter = android.graphics.BlurMaskFilter(glowingStrokeWidth.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }

                    val borderPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        this.shader = shader
                        // 使用 BlurMaskFilter 产生 glow 效果
                        strokeWidth = glowingStrokeWidth.toPx()
                    }

                    // 绘制圆角矩形边框
                    // 注意：边框绘制范围需要内缩半个边框宽度，以保证边框不被裁剪
                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        glowingStrokeWidth.toPx() / 2 * ratio,
                        glowingStrokeWidth.toPx() / 2,
                        canvasWidth - glowingStrokeWidth.toPx() / 2  * ratio,
                        canvasHeight - glowingStrokeWidth.toPx() / 2,
                        cornersRadius.toPx(),
                        cornersRadius.toPx(),
                        paint
                    )

                    drawContext.canvas.nativeCanvas.drawRoundRect(
                        glowingStrokeWidth.toPx()  * ratio,
                        glowingStrokeWidth.toPx() ,
                        canvasWidth - glowingStrokeWidth.toPx()  * ratio,
                        canvasHeight - glowingStrokeWidth.toPx()  ,
                        cornersRadius.toPx(),
                        cornersRadius.toPx(),
                        borderPaint
                    )
                }


            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}



