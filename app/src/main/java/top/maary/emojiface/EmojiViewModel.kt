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
    // 暂存检测结果，供后续多次调用 processDetections 使用
    private var detectionResult: DetectionResult? = null

    // LiveData 用于将处理后的 Bitmap 传递给 UI 层显示
    private val _outputBitmap = MutableLiveData<Bitmap>()
    val outputBitmap: LiveData<Bitmap> = _outputBitmap

    /**
     * 调用模型进行检测，将原始检测结果暂存。
     */
    fun detect(input: Bitmap) {
        val sessionOptions = OrtSession.SessionOptions().apply {
            registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        }
        model = application.resources.openRawResource(modelId).readBytes()
        ortSession = ortEnv.createSession(model, sessionOptions)

        viewModelScope.launch {
            detectionResult = faceDetector.detect(bitmapToInputStream(input), ortEnv, ortSession)
            // 初次处理检测结果，绘制 emoji，并更新 LiveData
            val processedBitmap = processDetections(input)
            _outputBitmap.postValue(processedBitmap)
        }
    }

    /**
     * 对传入的 Bitmap 根据已存储的检测结果绘制 emoji。
     * 每个检测目标采用其边界框中心为绘制中心，
     * 边界框对角线长度作为 emoji 尺寸，
     * 并根据左右眼计算出的人脸旋转角度对 emoji 进行旋转，
     * 同时随机选取一组预定义 emoji 中的一个。
     */
    fun processDetections(input: Bitmap): Bitmap {
        // 创建可修改的 Bitmap 用于绘制
        val mutableBitmap = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // 定义绘制 emoji 的 Paint
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        detectionResult?.detections?.forEach { detection ->
            // 假设检测结果格式为:
            // [x_center, y_center, width, height, confidence, classId, keypoints...]
            val xCenter = detection[0]
            val yCenter = detection[1]
            val width = detection[2]
            val height = detection[3]
            // 以边界框对角线长度作为 emoji 的直径
//            val diameter = Math.sqrt((width * width + height * height).toDouble()).toFloat()
//            val diameter = width.toFloat()
            val diagonal = Math.sqrt((width * width + height * height).toDouble()).toFloat()
            val diffRatio = kotlin.math.abs(width - height) / kotlin.math.max(width, height)
            // diffRatio 范围在 0 到 1 之间，我们可以将它作为权重直接用于插值
            val diameter = width * (1 - diffRatio) + diagonal * diffRatio


            // 提取关键点信息，假设从索引 6 开始，每个关键点 3 个数，5 个关键点
            val keypoints = Array(5) { FloatArray(3) }
            for (i in 0 until 5) {
                keypoints[i][0] = detection[6 + i * 3]     // x 坐标
                keypoints[i][1] = detection[6 + i * 3 + 1] // y 坐标
                keypoints[i][2] = detection[6 + i * 3 + 2] // 置信度
            }
            // 使用第一个和第二个关键点（假设分别为左眼和右眼）计算人脸的 roll 角度
            val leftEye = keypoints[0]
            val rightEye = keypoints[1]
            val angle = Math.toDegrees(
                kotlin.math.atan2(
                    (rightEye[1] - leftEye[1]).toDouble(),
                    (rightEye[0] - leftEye[0]).toDouble()
                )
            ).toFloat()

            // 从预定义 emoji 列表中随机选择一个
            val emojiOptions = listOf("😀", "😃", "😄", "😁")
            val randomEmoji = emojiOptions.random()

            // 调用封装好的函数绘制单个 emoji
            drawEmoji(canvas, xCenter, yCenter, diameter, angle, randomEmoji, emojiPaint)
        }

        return mutableBitmap
    }

    /**
     * 绘制单个 emoji：
     * 在指定中心位置绘制 emoji，使用指定直径（可作为文本大小）及旋转角度。
     *
     * @param canvas 目标 Canvas
     * @param centerX emoji 绘制中心的 X 坐标
     * @param centerY emoji 绘制中心的 Y 坐标
     * @param diameter 作为 emoji 尺寸的直径
     * @param rotationAngle emoji 的旋转角度（与人脸 roll 角度一致）
     * @param emoji 要绘制的 emoji 字符串
     * @param paint 用于绘制的 Paint 对象
     */
    private fun drawEmoji(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        diameter: Float,
        rotationAngle: Float,
        emoji: String,
        paint: Paint
    ) {
        // 根据直径设置文本大小
        paint.textSize = diameter
        // 保存当前 Canvas 状态
        canvas.save()
        // 旋转 Canvas，使得绘制的 emoji 方向与人脸朝向一致
        canvas.rotate(rotationAngle, centerX, centerY)
        // 为了使 emoji 居中绘制，需要计算 baseline 调整：
        // (centerY - (ascent + descent)/2) 可以使文本垂直居中
        canvas.drawText(emoji, centerX, centerY - (paint.ascent() + paint.descent()) / 2, paint)
        // 恢复 Canvas 状态
        canvas.restore()
    }

    private fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }
}
