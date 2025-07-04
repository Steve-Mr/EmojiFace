package top.maary.emojiface.domain.usecase

import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.ui.edit.model.BlurRegion
import javax.inject.Inject
import kotlin.math.atan2

class CalculateBlurRegionsUseCase @Inject constructor() {

    /**
     * 将原始检测结果转换为适用于模糊遮罩的、带角度的椭圆区域列表。
     * @param detectionOutput 来自 DetectFacesUseCase 的原始输出。
     * @return Result<List<BlurRegion>> 包含所有面部区域（矩形+角度）的列表。
     */
    suspend operator fun invoke(detectionOutput: DetectionOutput): Result<List<BlurRegion>> = withContext(Dispatchers.Default) {
        runCatching {
            val detections = detectionOutput.detectionResult.detections
            val scaleFactorX = detectionOutput.scaleFactorX
            val scaleFactorY = detectionOutput.scaleFactorY

            detections.map { detection ->
                // 复用坐标转换逻辑
                val xCenter = detection[0] * scaleFactorX
                val yCenter = detection[1] * scaleFactorY
                val width = detection[2] * scaleFactorX
                val height = detection[3] * scaleFactorY

                val rect = RectF(
                    xCenter - width / 2,
                    yCenter - height / 2,
                    xCenter + width / 2,
                    yCenter + height / 2
                )

                // --- 借鉴自 Emoji 部分的角度计算逻辑 ---
                val keypoints = Array(5) { FloatArray(3) }
                for (i in 0 until 5) {
                    keypoints[i][0] = detection[6 + i * 3] * scaleFactorX
                    keypoints[i][1] = detection[6 + i * 3 + 1] * scaleFactorY
                }
                val leftEye = keypoints[0]
                val rightEye = keypoints[1]
                val angle = Math.toDegrees(
                    atan2(
                        (rightEye[1] - leftEye[1]).toDouble(),
                        (rightEye[0] - leftEye[0]).toDouble()
                    )
                ).toFloat()

                // 返回新的数据结构
                BlurRegion(rect = rect, angle = angle)
            }
        }
    }
}