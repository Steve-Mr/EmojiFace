package top.maary.emojiface.domain.usecase

import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CalculateBlurRegionsUseCase @Inject constructor() {

    /**
     * 将原始检测结果转换为适用于模糊遮罩的矩形区域列表。
     * @param detectionOutput 来自 DetectFacesUseCase 的原始输出。
     * @return Result<List<RectF>> 包含所有面部矩形框的列表。
     */
    suspend operator fun invoke(detectionOutput: DetectionOutput): Result<List<RectF>> = withContext(Dispatchers.Default) {
        runCatching {
            val detections = detectionOutput.detectionResult.detections
            val scaleFactorX = detectionOutput.scaleFactorX
            val scaleFactorY = detectionOutput.scaleFactorY

            // 复用坐标转换逻辑，将检测到的每个面部转换为一个 RectF
            detections.map { detection ->
                // [x_center, y_center, width, height, ...]
                val xCenter = detection[0] * scaleFactorX
                val yCenter = detection[1] * scaleFactorY
                val width = detection[2] * scaleFactorX
                val height = detection[3] * scaleFactorY

                // 计算矩形的左上角和右下角
                RectF(
                    xCenter - width / 2,
                    yCenter - height / 2,
                    xCenter + width / 2,
                    yCenter + height / 2
                )
            }
        }
    }
}