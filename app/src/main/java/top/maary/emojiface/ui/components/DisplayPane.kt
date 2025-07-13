package top.maary.emojiface.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import top.maary.emojiface.R
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.EditScreenActions
import top.maary.emojiface.ui.edit.state.EditScreenState

@Composable
fun DisplayPane(
    modifier: Modifier,
    state: EditScreenState,
    actions: EditScreenActions,
    editingEmoji: EmojiDetection?,
    editingBlurRegion: BlurRegion?) {
    // --- 圖片顯示區域 ---
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (state.displayedBitmap != null) {

            val cornerRadius = 16.dp
            val verticalPadding = 8.dp
            val horizontalPadding = (state.aspectRatio ?: 1f) * 8f.dp

            // 准备一个用于覆盖层的 Box，它的大小将和图片容器一致
            Box(
                modifier = Modifier
                    .aspectRatio(state.aspectRatio ?: 1f)
                    .fillMaxSize()
            ) {

                // 1. 将原先 Image 的特定逻辑提取到一个 Modifier 变量中
                val imageSpecificModifier = Modifier
                    .onGloballyPositioned { layoutCoordinates ->
                        // 回報容器尺寸
                        actions.onImageContainerMeasured(layoutCoordinates.size)
                    }
                    .then(
                        if (state.isAddMode) {
                            Modifier.pointerInput(Unit) { // 合并 pointerInput
                                detectTapGestures { offset ->
                                    val containerWidth = state.imageContainerSize.width
                                    val containerHeight = state.imageContainerSize.height
                                    val displayedBitmapWidth = state.displayedBitmap.width
                                    val displayedBitmapHeight = state.displayedBitmap.height

                                    if (containerWidth > 0 && containerHeight > 0) {
                                        val scaleX =
                                            displayedBitmapWidth.toFloat() / containerWidth
                                        val scaleY =
                                            displayedBitmapHeight.toFloat() / containerHeight
                                        val originalX = offset.x * scaleX
                                        val originalY = offset.y * scaleY
                                        actions.onImageTapToAdd(
                                            Offset(
                                                originalX,
                                                originalY
                                            )
                                        )
                                    } else {
                                        actions.onImageTapToAdd(offset)
                                    }
                                }
                            }
                        } else Modifier
                    )

                // 2. 使用重构后的 ResultImg
                ResultImg(
                    modifier = Modifier
                        .aspectRatio(state.aspectRatio ?: 1f)
                        .fillMaxSize(), // GlowingCard 的 Modifier
                    bitmap = state.displayedBitmap,
                    description = stringResource(R.string.process_result),
                    ratio = state.aspectRatio ?: 1f,
                    animate = state.isProcessing,
                    imageModifier = imageSpecificModifier // 3. 将提取的 Modifier 传入
                )

                EmojiOverlay(
                    state = state,
                    actions = actions, // <-- 传入 actions
                    editingEmoji = editingEmoji,
                    editingBlurRegion = editingBlurRegion, // <-- 传入新增的 editingBlurRegion
                    padding = PaddingValues(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding
                    ),
                    cornerRadius = cornerRadius,
                )
            }

        } else {
            // 沒有圖片時顯示選擇圖片按鈕 (这部分逻辑不变)
            ExtendedFloatingActionButton(
                onClick = actions.onPickImageClick,
                icon = {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.choose_image)
                    )
                },
                text = { Text(text = stringResource(R.string.choose_image)) },
            )
        }
    }
}