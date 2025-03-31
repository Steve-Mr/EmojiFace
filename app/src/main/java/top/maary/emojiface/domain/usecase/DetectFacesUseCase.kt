package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.facedetection.DetectionResult
import top.maary.emojiface.facedetection.YoloPoseDetector
import top.maary.emojiface.util.bitmapToInputStream
import top.maary.emojiface.util.scaleBitmapIfNeeded
import javax.inject.Inject

// 定义输出数据结构
data class DetectionOutput(
    val originalBitmap: Bitmap,
    val detectionResult: DetectionResult,
    val scaleFactorX: Float,
    val scaleFactorY: Float
)

class DetectFacesUseCase @Inject constructor(
    private val faceDetector: YoloPoseDetector // 注入检测器实例
) {
    suspend operator fun invoke(originalBitmap: Bitmap): Result<DetectionOutput> = withContext(Dispatchers.IO) {
        runCatching { // 使用 runCatching 简化 try-catch 和 Result 返回

                // 1. 缩放图片以提高模型推理效率
                val scaledBitmap = scaleBitmapIfNeeded(originalBitmap)

                // 2. 计算缩放因子，用于后续坐标转换
                val scaleFactorX = originalBitmap.width.toFloat() / scaledBitmap.width.toFloat()
                val scaleFactorY = originalBitmap.height.toFloat() / scaledBitmap.height.toFloat()

                // 3. 执行人脸检测 (假设 detect 方法需要 InputStream)
                // 注意：YoloPoseDetector 现在需要能接受 Bitmap 或 InputStream，并管理 OrtSession
                // 这里的实现需要根据 YoloPoseDetector 的具体情况调整
                val detectionResult = faceDetector.detect(bitmapToInputStream(scaledBitmap)) // 假设 detect 接受 InputStream

                DetectionOutput(
                    originalBitmap = originalBitmap,
                    detectionResult = detectionResult,
                    scaleFactorX = scaleFactorX,
                    scaleFactorY = scaleFactorY
                )
        }
    }
}
