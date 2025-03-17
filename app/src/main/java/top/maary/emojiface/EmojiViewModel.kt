package top.maary.emojiface

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class EmojiViewModel @Inject constructor(
    @ApplicationContext private val application: Context
) : ViewModel() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private lateinit var model: ByteArray
    private val modelId = R.raw.face
    private val faceDetector = YoloPoseDetector()
    private lateinit var detectionResult: DetectionResult

    // LiveData 用于将处理后的 Bitmap 传递给 UI 层显示
    private val _outputBitmap = MutableLiveData<Bitmap>()
    val outputBitmap: LiveData<Bitmap> = _outputBitmap

    fun detect(input: Bitmap){

        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())

        model = application.resources.openRawResource(modelId).readBytes()
        ortSession = ortEnv.createSession(model, sessionOptions)

        viewModelScope.launch {
            detectionResult = faceDetector.detect(bitmapToInputStream(input), ortEnv, ortSession)
            val processedBitmap = processDetections(input, detectionResult.detections)
            // 更新 LiveData，UI 层可以观察到变化
            _outputBitmap.postValue(processedBitmap)
        }
    }

    private fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    /**
     * 处理模型的检测结果：
     *  - 假设每个检测结果包含 21 个数值：
     *      [x_center, y_center, width, height, confidence, class_id, keypoints...]
     *  - 前 4 个数用于计算边界框（中心点 + 宽高转化为左上/右下坐标）
     *  - 第 5 个数表示置信度，第 6 个表示类别
     *  - 剩下 15 个数为 5 个关键点的数据（每个关键点 3 个数：x, y, confidence）
     */
    private fun processDetections(inputBitmap: Bitmap, detections: Array<FloatArray>): Bitmap {
        // 创建一个可修改的 Bitmap 用于绘制
        val mutableBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // 绘制边框的 Paint
        val rectPaint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        // 绘制文本的 Paint
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 32f
        }
        // 绘制关键点和箭头的 Paint
        val pointPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
        }

        // 遍历每个检测结果
        for (detection in detections) {
            // 解析边界框数据：假设格式为 [x_center, y_center, width, height]
            val xCenter = detection[0]
            val yCenter = detection[1]
            val width = detection[2]
            val height = detection[3]
            val confidence = detection[4]
            // 如果需要可以使用 classId = detection[5].toInt()

            // 转换中心坐标为左上角和右下角坐标
            val x1 = (xCenter - width / 2).toInt()
            val y1 = (yCenter - height / 2).toInt()
            val x2 = (xCenter + width / 2).toInt()
            val y2 = (yCenter + height / 2).toInt()

            // 绘制边框
            canvas.drawRect(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), rectPaint)
            // 绘制置信度文本
            canvas.drawText("%.2f".format(confidence), x1.toFloat(), (y1 - 10).toFloat(), textPaint)

            // 解析关键点信息：从索引 6 开始，总共 15 个数，5 个关键点，每个 (x, y, conf)
            val keypoints = Array(5) { FloatArray(3) }
            for (i in 0 until 5) {
                keypoints[i][0] = detection[6 + i * 3]     // x 坐标
                keypoints[i][1] = detection[6 + i * 3 + 1] // y 坐标
                keypoints[i][2] = detection[6 + i * 3 + 2] // 关键点置信度
                // 绘制关键点
                canvas.drawCircle(keypoints[i][0], keypoints[i][1], 4f, pointPaint)
            }

            // 根据左右眼关键点计算人脸的 roll 角度（假设 keypoints[0] 为左眼，keypoints[1] 为右眼）
            val leftEye = keypoints[0]
            val rightEye = keypoints[1]
            val angle = Math.toDegrees(
                kotlin.math.atan2(
                    (rightEye[1] - leftEye[1]).toDouble(),
                    (rightEye[0] - leftEye[0]).toDouble()
                )
            )

            // 计算左右眼中点，作为箭头起点
            val midX = (leftEye[0] + rightEye[0]) / 2
            val midY = (leftEye[1] + rightEye[1]) / 2
            val arrowLength = 50f
            val angleRad = Math.toRadians(angle)
            val endX = midX + arrowLength * Math.cos(angleRad).toFloat()
            val endY = midY + arrowLength * Math.sin(angleRad).toFloat()

            // 绘制表示人脸朝向的箭头
            canvas.drawLine(midX, midY, endX, endY, pointPaint)
            // 绘制角度文本
            canvas.drawText("Roll: %.2f".format(angle), midX, midY, textPaint)
        }
        return mutableBitmap
    }

}