package top.maary.emojiface.domain.usecase

import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository.Companion.MOSAIC_TARGET_EYES
import top.maary.emojiface.ui.edit.model.BlurRegion
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.hypot

class CalculateBlurRegionsUseCase @Inject constructor() {

    /**
     * 将原始检测结果转换为适用于模糊遮罩的、带角度的椭圆区域列表。
     * @param detectionOutput 来自 DetectFacesUseCase 的原始输出。
     * @return Result<List<BlurRegion>> 包含所有面部区域（矩形+角度）的列表。
     */
    suspend operator fun invoke(
        detectionOutput: DetectionOutput,
        mosaicTarget: Int
    ): Result<List<BlurRegion>> = withContext(Dispatchers.Default) {
        runCatching {
            val detections = detectionOutput.detectionResult.detections
            val scaleFactorX = detectionOutput.scaleFactorX
            val scaleFactorY = detectionOutput.scaleFactorY

            detections.map { detection ->

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

                val rect = when (mosaicTarget) {
                    MOSAIC_TARGET_EYES -> {
                        // --- 眼部模式的矩形计算 ---
                        // 1. 计算两眼之间的距离作为宽度
                        val eyesDistance = hypot(
                            (rightEye[0] - leftEye[0]).toDouble(),
                            (rightEye[1] - leftEye[1]).toDouble()
                        ).toFloat()

                        // 2. 将宽度扩大一点以完全覆盖眼睛（例如，乘以一个系数）
                        val rectWidth = eyesDistance * 1.8f
                        // 3. 高度可以设为宽度的固定比例，以形成合适的椭圆
                        val rectHeight = rectWidth * 0.5f

                        // 4. 计算矩形中心点（两眼中心）
                        val centerX = (leftEye[0] + rightEye[0]) / 2
                        val centerY = (leftEye[1] + rightEye[1]) / 2

                        // 5. 构建矩形
                        RectF(
                            centerX - rectWidth / 2,
                            centerY - rectHeight / 2,
                            centerX + rectWidth / 2,
                            centerY + rectHeight / 2
                        )
                    }
                    else -> { // 默认为 MOSAIC_TARGET_FACE
                        // --- 面部模式的矩形计算 (复用已有逻辑) ---
                        val xCenter = detection[0] * scaleFactorX
                        val yCenter = detection[1] * scaleFactorY
                        val width = detection[2] * scaleFactorX
                        val height = detection[3] * scaleFactorY

                        RectF(
                            xCenter - width / 2,
                            yCenter - height / 2,
                            xCenter + width / 2,
                            yCenter + height / 2
                        )
                    }
                }



                // 返回新的数据结构
                BlurRegion(rect = rect, angle = angle)
            }
        }
    }
}