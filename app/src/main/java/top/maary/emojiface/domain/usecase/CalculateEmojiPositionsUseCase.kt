package top.maary.emojiface.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.ui.edit.model.EmojiDetection
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

class CalculateEmojiPositionsUseCase @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) {
    suspend operator fun invoke(detectionOutput: DetectionOutput): Result<List<EmojiDetection>> = withContext(Dispatchers.Default) { // 计算密集型，用 Default
        runCatching {
            var availableEmojis = preferenceRepository.emojiOptionsFlow.first() // 获取当前的 emoji 列表
            if (availableEmojis.isEmpty()) {
                // 可以选择抛出错误或使用默认列表
                // throw IllegalStateException("Emoji options list is empty.")
                availableEmojis = PreferenceRepository.DEFAULT_EMOJI_LIST
            }
            val remainingEmojiOptions = availableEmojis.toMutableList()

            val detections = detectionOutput.detectionResult.detections
            val scaleFactorX = detectionOutput.scaleFactorX
            val scaleFactorY = detectionOutput.scaleFactorY

            val sortedDetections = detections.sortedBy { it[0] } // 按 x 坐标排序
            val selectedEmojiList = mutableListOf<EmojiDetection>()

            sortedDetections.forEach { detection ->
                // 转换坐标到原图尺寸
                val xCenter = detection[0] * scaleFactorX
                val yCenter = detection[1] * scaleFactorY
                val width = detection[2] * scaleFactorX
                val height = detection[3] * scaleFactorY

                // 计算直径 (保持 ViewModel 中的逻辑)
                val diagonal = hypot(width.toDouble(), height.toDouble()).toFloat()
                val diffRatio = abs(width - height) / max(width, height)
                val diameter = width * (1 - diffRatio) + diagonal * diffRatio

                // 处理关键点坐标和角度计算 (保持 ViewModel 中的逻辑)
                val keypoints = Array(5) { FloatArray(3) }
                for (i in 0 until 5) {
                    keypoints[i][0] = detection[6 + i * 3] * scaleFactorX
                    keypoints[i][1] = detection[6 + i * 3 + 1] * scaleFactorY
                    // keypoints[i][2] = detection[6 + i * 3 + 2] // 置信度，这里没用到
                }
                val leftEye = keypoints[0]
                val rightEye = keypoints[1]
                val angle = Math.toDegrees(
                    atan2(
                        (rightEye[1] - leftEye[1]).toDouble(),
                        (rightEye[0] - leftEye[0]).toDouble()
                    )
                ).toFloat()

                // 选择 Emoji
                if (remainingEmojiOptions.isEmpty()) {
                    remainingEmojiOptions.addAll(availableEmojis) // 如果用完了就重新填满
                }
                val chosenEmoji = remainingEmojiOptions.random()
                remainingEmojiOptions.remove(chosenEmoji)

                selectedEmojiList.add(
                    EmojiDetection(
                        xCenter = xCenter,
                        yCenter = yCenter,
                        diameter = diameter,
                        angle = angle,
                        emoji = chosenEmoji
                    )
                )
            }
            selectedEmojiList
        }
    }
}
