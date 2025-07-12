package top.maary.emojiface.ui.components

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
import top.maary.emojiface.util.createBlurredRegionBitmap
import top.maary.emojiface.util.createHalftoneRegionBitmap
import top.maary.emojiface.util.createPixelatedRegionBitmap
import top.maary.emojiface.util.isPointInRotatedEllipse
import kotlin.math.pow

@Composable
fun EmojiOverlay(
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?,
    padding: PaddingValues,
    cornerRadius: Dp,
) {
    // 获取原始图片尺寸和屏幕上容器的尺寸，用于坐标和大小的缩放
    val originalWidth = state.displayedBitmap?.width?.toFloat() ?: return
    val containerWidth = state.imageContainerSize.width.toFloat()
    val containerHeight = state.imageContainerSize.height.toFloat()

    if (containerWidth == 0f || containerHeight == 0f) return

    // 计算缩放比例
    val scale = containerWidth / originalWidth

    val density = LocalDensity.current
    val horizontalPaddingPx = with(density) { padding.calculateLeftPadding(LayoutDirection.Ltr).toPx() }
    val verticalPaddingPx = with(density) { padding.calculateTopPadding().toPx() }

    val bitmapCache = remember { mutableMapOf<BlurRegion, ImageBitmap>() }
    LaunchedEffect(state.blurRegions, state.mosaicType) {
        bitmapCache.clear()
    }

    // 合并固定列表和正在编辑的临时状态，用于统一渲染
    val emojisToRender = remember(state.emojiDetections, editingEmoji) {
        val list = state.emojiDetections.toMutableList()
        if (editingEmoji != null) {
            val editingIndex =
                list.indexOfFirst { it.xCenter == editingEmoji.xCenter && it.yCenter == editingEmoji.yCenter }
                    .takeIf { it != -1 }
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

    val regionsToRender = remember(state.blurRegions, editingBlurRegion) {
        val list = state.blurRegions.toMutableList()
        if (editingBlurRegion != null) {
            val editingIndex = state.editingBlurRegionIndex
            // 如果 index 有效且在列表范围内，则替换
            if (editingIndex != null && editingIndex >= 0 && editingIndex < list.size) {
                list[editingIndex] = editingBlurRegion
            } else {
                // 否则 (index 为 -1 或 null)，则为新增，直接添加到列表末尾用于渲染
                list.add(editingBlurRegion)
            }
        }
        // **--- 结束修改 ---**
        list
    }

    // 创建一个可以在重组间复用的 Paint 对象
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
    }

    val boxPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f // 边框宽度
            color = Color.GREEN // 边框颜色
        }
    }
    val textBgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.GREEN // 文字背景颜色
        }
    }
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.BLACK // 文字颜色
            textSize = 32f // 文字大小
        }
    }

    val previewPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f // 预览框边框宽度
            color = Color.WHITE // 使用高对比度的白色
        }
    }
    val previewFillPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            // 半透明填充，让用户能看到下方的图片内容
            color = Color.argb(80, 255, 255, 255)
        }
    }

    // 使用 Canvas Composable 进行绘制
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if(!state.isAddMode){
                    Modifier.pointerInput(state.mosaicMode, emojisToRender, regionsToRender) {
                        detectTapGestures { offset ->
                            // 1. 检查点击是否在有效内容区域内
                            if (offset.x < horizontalPaddingPx || offset.x > containerWidth - horizontalPaddingPx ||
                                offset.y < verticalPaddingPx || offset.y > state.imageContainerSize.height - verticalPaddingPx
                            ) {
                                return@detectTapGestures // 点击在 padding 上，不处理
                            }

                            // 2. 将屏幕点击坐标转换为原始图片坐标
                            val originalTapPoint = PointF(
                                (offset.x - horizontalPaddingPx) / scale,
                                (offset.y - verticalPaddingPx) / scale
                            )

                            // 3. 根据当前模式，遍历并检测点击
                            if (state.mosaicMode == PreferenceRepository.MOSAIC_MODE_EMOJI) {
                                // 从上层开始遍历 (因此用 reversed)
                                val emojiIndex = emojisToRender.indices.reversed().firstOrNull { index ->
                                    val emoji = emojisToRender[index]
                                    // 对圆形来说，旋转不影响点到中心的距离判断
                                    val dx = originalTapPoint.x - emoji.xCenter
                                    val dy = originalTapPoint.y - emoji.yCenter
                                    (dx * dx + dy * dy) < (emoji.diameter / 2).pow(2)
                                }
                                if (emojiIndex != null) {
                                    actions.onEmojiCardClick(emojiIndex)
                                }
                            } else { // MOSAIC_MODE_BLUR
                                // 从上层开始遍历
                                val regionIndex = regionsToRender.indices.reversed().firstOrNull { index ->
                                    val region = regionsToRender[index]
                                    isPointInRotatedEllipse(originalTapPoint, region)
                                }
                                if (regionIndex != null) {
                                    actions.onBlurRegionSelected(regionIndex)
                                }
                            }
                        }
                    }
                } else { Modifier }
            )

    ) {

        when (state.mosaicMode) {
            PreferenceRepository.MOSAIC_MODE_EMOJI -> {
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

            PreferenceRepository.MOSAIC_MODE_BLUR -> {
                val sourceBitmap = state.displayedBitmap.asAndroidBitmap()

                regionsToRender.forEachIndexed { index, region ->

                    val isPreviewing = state.isSliding && region == editingBlurRegion

                    val destinationRect = RectF(
                        region.rect.left * scale,
                        region.rect.top * scale,
                        region.rect.right * scale,
                        region.rect.bottom * scale
                    )

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.rotate(region.angle, destinationRect.centerX(), destinationRect.centerY())

                        val ovalPath = Path().apply { addOval(destinationRect, Path.Direction.CW) }

                        if (isPreviewing) {
                            // --- A. 如果是预览模式，绘制轻量级的预览框 ---
                            canvas.nativeCanvas.drawPath(ovalPath, previewFillPaint) // 半透明填充
                            canvas.nativeCanvas.drawPath(ovalPath, previewPaint)     // 白色描边
                        } else {
                            // --- B. 正常模式，绘制真实的模糊效果 ---
                            val regionBitmap = bitmapCache.getOrPut(region) {
                                when (state.mosaicType) {
                                    PreferenceRepository.MOSAIC_TYPE_PIXELATED -> createPixelatedRegionBitmap(sourceBitmap, region)
                                    PreferenceRepository.MOSAIC_TYPE_HALFTONE -> createHalftoneRegionBitmap(sourceBitmap, region)
                                    else -> createBlurredRegionBitmap(sourceBitmap, region)
                                }.asImageBitmap()
                            }
                            canvas.nativeCanvas.clipPath(ovalPath)
                            drawImage(
                                image = regionBitmap,
                                dstOffset = IntOffset(destinationRect.left.toInt(), destinationRect.top.toInt()),
                                dstSize = IntSize(destinationRect.width().toInt(), destinationRect.height().toInt())
                            )
                        }
                        canvas.nativeCanvas.restore()
                    }
                }
            }
        }

        state.fakeDetections.forEach { fake ->
            // 缩放边界框坐标
            val scaledBox = RectF(
                fake.box.left * scale,
                fake.box.top * scale,
                fake.box.right * scale,
                fake.box.bottom * scale
            )
            // 绘制矩形框
            drawContext.canvas.nativeCanvas.drawRect(scaledBox, boxPaint)

            // 准备要绘制的文字
            val labelText =
                "${fake.label} ${"%.2f".format(fake.confidence)} age:${fake.startAge}-${fake.endAge}"

            // 测量文字宽度以绘制背景
            val textWidth = textPaint.measureText(labelText)
            val textBounds = Rect()
            textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            val textHeight = textBounds.height()

            // 在框的左上角绘制文字和背景
            val textBgRect = RectF(
                scaledBox.left,
                scaledBox.top - textHeight - 8f, // 向上偏移一点
                scaledBox.left + textWidth + 8f,
                scaledBox.top
            )
            drawContext.canvas.nativeCanvas.drawRect(textBgRect, textBgPaint)
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                scaledBox.left + 4f,
                scaledBox.top - 4f,
                textPaint
            )
        }
    }
}