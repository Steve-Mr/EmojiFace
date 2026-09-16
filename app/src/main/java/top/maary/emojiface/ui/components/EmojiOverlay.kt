package top.maary.emojiface.ui.components

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import top.maary.emojiface.domain.model.geometry.AnchorTarget
import top.maary.emojiface.domain.model.geometry.Point2D
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.model.occlusion.OcclusionMode
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState
import top.maary.emojiface.util.createBlurredRegionBitmap
import top.maary.emojiface.util.createHalftoneRegionBitmap
import top.maary.emojiface.util.createPixelatedRegionBitmap
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun EmojiOverlay(
    state: EditScreenState,
    actions: EditScreenActions,
    editingOcclusion: FaceOcclusion? = state.editingOcclusion,
    padding: PaddingValues,
    cornerRadius: Dp,
) {
    val originalWidth = state.displayedBitmap?.width?.toFloat() ?: return
    val containerWidth = state.imageContainerSize.width.toFloat()
    val containerHeight = state.imageContainerSize.height.toFloat()

    if (containerWidth == 0f || containerHeight == 0f) return

    val scale = containerWidth / originalWidth

    val density = LocalDensity.current
    val horizontalPaddingPx = with(density) { padding.calculateLeftPadding(LayoutDirection.Ltr).toPx() }
    val verticalPaddingPx = with(density) { padding.calculateTopPadding().toPx() }

    val blurBitmapCache = remember { mutableMapOf<String, ImageBitmap>() }
    LaunchedEffect(state.occlusions, state.mosaicType, state.displayedBitmap) {
        blurBitmapCache.clear()
    }

    val occlusionsToRender = remember(state.occlusions, editingOcclusion) {
        val list = state.occlusions.toMutableList()
        if (editingOcclusion != null) {
            val editingIndex = state.editingOcclusionIndex
            if (editingIndex != null && editingIndex >= 0 && editingIndex < list.size) {
                list[editingIndex] = editingOcclusion
            } else {
                list.add(editingOcclusion)
            }
        }
        list
    }

    val emojiPaint = remember {
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
            strokeWidth = 4f
            color = Color.GREEN
        }
    }
    val textBgPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.GREEN
        }
    }
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.BLACK
            textSize = 32f
        }
    }

    val previewPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.WHITE
        }
    }
    val previewFillPaint = remember {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.argb(80, 255, 255, 255)
        }
    }

    val bitmapPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (!state.isAddMode) {
                    Modifier.pointerInput(occlusionsToRender) {
                        detectTapGestures { offset ->
                            if (offset.x < horizontalPaddingPx || offset.x > containerWidth - horizontalPaddingPx ||
                                offset.y < verticalPaddingPx || offset.y > state.imageContainerSize.height - verticalPaddingPx
                            ) {
                                return@detectTapGestures
                            }

                            val originalTapPoint = Point2D(
                                (offset.x - horizontalPaddingPx) / scale,
                                (offset.y - verticalPaddingPx) / scale
                            )

                            val selectedIndex = occlusionsToRender.indices.reversed().firstOrNull { index ->
                                val occlusion = occlusionsToRender[index]
                                val treatAsEllipse = (occlusion.mode !is OcclusionMode.Avatar ||
                                        occlusion.mode.cropShape == OcclusionMode.Avatar.AvatarCropShape.Circle)
                                occlusion.geometry.contains(originalTapPoint, treatAsEllipse = treatAsEllipse)
                            }
                            if (selectedIndex != null) {
                                actions.onOcclusionSelected(selectedIndex)
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        occlusionsToRender.forEach { occlusion ->
            val geom = occlusion.geometry
            val centerX = geom.center.x * scale
            val centerY = geom.center.y * scale
            val effWidth = geom.effectiveSize.width * scale
            val effHeight = geom.effectiveSize.height * scale
            val diameter = geom.effectiveSize.maxDimension * scale
            val halfDim = diameter / 2f
            val rotation = geom.rotationDegrees

            when (val mode = occlusion.mode) {
                is OcclusionMode.Emoji -> {
                    emojiPaint.typeface = state.typeface ?: Typeface.DEFAULT
                    emojiPaint.textSize = diameter
                    val verticalOffset = (emojiPaint.descent() + emojiPaint.ascent()) / 2f

                    rotate(
                        degrees = rotation,
                        pivot = Offset(centerX, centerY)
                    ) {
                        drawContext.canvas.nativeCanvas.drawText(
                            mode.emoji,
                            centerX,
                            centerY - verticalOffset,
                            emojiPaint
                        )
                    }
                }

                is OcclusionMode.Blur -> {
                    val isPreviewing = state.isSliding && occlusion == editingOcclusion
                    val regionRect = if (mode.target == OcclusionMode.Blur.BlurTarget.Eyes && geom.anchorTarget != AnchorTarget.EYES) {
                        val eff = geom.effectiveSize
                        val eyeWidth = eff.width * 0.75f
                        val eyeHeight = eyeWidth * 0.35f
                        val rad = Math.toRadians(geom.rotationDegrees.toDouble())
                        val offsetDist = -eff.height * 0.1f
                        val eyeCenterX = geom.center.x + (offsetDist * -sin(rad)).toFloat()
                        val eyeCenterY = geom.center.y + (offsetDist * cos(rad)).toFloat()
                        RectF(
                            eyeCenterX - eyeWidth / 2f,
                            eyeCenterY - eyeHeight / 2f,
                            eyeCenterX + eyeWidth / 2f,
                            eyeCenterY + eyeHeight / 2f
                        )
                    } else {
                        geom.toRectF()
                    }

                    val destinationRect = RectF(
                        regionRect.left * scale,
                        regionRect.top * scale,
                        regionRect.right * scale,
                        regionRect.bottom * scale
                    )

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.rotate(rotation, destinationRect.centerX(), destinationRect.centerY())

                        val ovalPath = Path().apply { addOval(destinationRect, Path.Direction.CW) }

                        if (isPreviewing) {
                            canvas.nativeCanvas.drawPath(ovalPath, previewFillPaint)
                            canvas.nativeCanvas.drawPath(ovalPath, previewPaint)
                        } else {
                            val sourceBitmap = state.displayedBitmap?.asAndroidBitmap()
                            if (sourceBitmap != null && regionRect.width() > 0f && regionRect.height() > 0f) {
                                val blurRegion = BlurRegion(rect = regionRect, angle = geom.rotationDegrees)
                                val cacheKey = "${occlusion.id}_${regionRect.left}_${regionRect.top}_${regionRect.width()}_${regionRect.height()}_${geom.rotationDegrees}_${mode.type}_${mode.target}"
                                val regionBitmap = blurBitmapCache.getOrPut(cacheKey) {
                                    when (mode.type) {
                                        OcclusionMode.Blur.BlurType.Pixelated -> createPixelatedRegionBitmap(sourceBitmap, blurRegion)
                                        OcclusionMode.Blur.BlurType.Halftone -> createHalftoneRegionBitmap(sourceBitmap, blurRegion)
                                        else -> createBlurredRegionBitmap(sourceBitmap, blurRegion)
                                    }.asImageBitmap()
                                }
                                canvas.nativeCanvas.clipPath(ovalPath)
                                drawImage(
                                    image = regionBitmap,
                                    dstOffset = IntOffset(destinationRect.left.toInt(), destinationRect.top.toInt()),
                                    dstSize = IntSize(destinationRect.width().toInt(), destinationRect.height().toInt())
                                )
                            }
                        }
                        canvas.nativeCanvas.restore()
                    }
                }

                is OcclusionMode.Avatar -> {
                    val avatarBitmap = state.cachedAvatarBitmaps[mode.avatarId]
                    if (avatarBitmap != null && halfDim > 0f) {
                        val destinationRect = RectF(
                            centerX - halfDim,
                            centerY - halfDim,
                            centerX + halfDim,
                            centerY + halfDim
                        )
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.rotate(rotation, centerX, centerY)

                            val clipPath = Path().apply {
                                when (mode.cropShape) {
                                    OcclusionMode.Avatar.AvatarCropShape.Circle -> {
                                        addOval(destinationRect, Path.Direction.CW)
                                    }
                                    OcclusionMode.Avatar.AvatarCropShape.RoundedRect -> {
                                        val cornerRadiusPx = destinationRect.width() * mode.cornerRadiusRatio
                                        addRoundRect(destinationRect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
                                    }
                                    OcclusionMode.Avatar.AvatarCropShape.Square -> {
                                        addRect(destinationRect, Path.Direction.CW)
                                    }
                                }
                            }
                            canvas.nativeCanvas.clipPath(clipPath)

                            val srcDim = min(avatarBitmap.width, avatarBitmap.height)
                            if (srcDim > 0) {
                                val srcLeft = (avatarBitmap.width - srcDim) / 2
                                val srcTop = (avatarBitmap.height - srcDim) / 2
                                val srcRect = Rect(srcLeft, srcTop, srcLeft + srcDim, srcTop + srcDim)
                                canvas.nativeCanvas.drawBitmap(avatarBitmap, srcRect, destinationRect, bitmapPaint)
                            }
                            canvas.nativeCanvas.restore()
                        }
                    }
                }
            }
        }

        state.fakeDetections.forEach { fake ->
            val scaledBox = RectF(
                fake.box.left * scale,
                fake.box.top * scale,
                fake.box.right * scale,
                fake.box.bottom * scale
            )
            drawContext.canvas.nativeCanvas.drawRect(scaledBox, boxPaint)

            val labelText = "${fake.label} ${"%.2f".format(fake.confidence)} age:${fake.startAge}-${fake.endAge}"
            val textWidth = textPaint.measureText(labelText)
            val textBounds = Rect()
            textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            val textHeight = textBounds.height()

            val textBgRect = RectF(
                scaledBox.left,
                scaledBox.top - textHeight - 8f,
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