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

    // LiveData 保存每个检测目标对应的选取的 emoji 顺序
    private val _selectedEmojis = MutableLiveData<List<EmojiDetection>>()
    val selectedEmojis: LiveData<List<EmojiDetection>> = _selectedEmojis

    private lateinit var base: Bitmap

    /**
     * 调用模型进行检测，并暂存检测结果。
     */
    fun detect(input: Bitmap) {
        base = input
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
     * 根据已存储的检测结果，对输入 Bitmap 绘制 emoji：
     * - 每个检测目标采用边界框中心作为绘制中心，
     * - 使用加权平均计算 emoji 的直径：当宽高接近时以宽度为准，否则平滑过渡到对角线值，
     * - 根据左右眼计算人脸的 roll 角度对 emoji 进行旋转，
     * - 从预定义列表中随机选取 emoji，但避免连续重复。
     *
     * 同时将选取的 emoji 顺序保存在 _selectedEmojis 中，供 UI 展示。
     */
    /**
     * 对传入的 Bitmap 根据检测结果绘制 emoji，并构造 EmojiDetection 列表
     */
    fun processDetections(input: Bitmap): Bitmap {
        val mutableBitmap = input.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val emojiOptions = listOf("😂", "😎", "😆", "😋", "🫡", "😊", "😜", "🤠")
        val selectedEmojiList = mutableListOf<EmojiDetection>()
        val sortedDetections = detectionResult?.detections?.sortedBy { it[0] } ?: emptyList()
        val remainingEmojiOptions = emojiOptions.toMutableList()

        sortedDetections.forEach { detection ->
            val xCenter = detection[0]
            val yCenter = detection[1]
            val width = detection[2]
            val height = detection[3]
            val diagonal = Math.sqrt((width * width + height * height).toDouble()).toFloat()
            val diffRatio = kotlin.math.abs(width - height) / kotlin.math.max(width, height)
            val diameter = width * (1 - diffRatio) + diagonal * diffRatio

            // 解析关键点并计算旋转角度（示例中使用左眼、右眼）
            val keypoints = Array(5) { FloatArray(3) }
            for (i in 0 until 5) {
                keypoints[i][0] = detection[6 + i * 3]
                keypoints[i][1] = detection[6 + i * 3 + 1]
                keypoints[i][2] = detection[6 + i * 3 + 2]
            }
            val leftEye = keypoints[0]
            val rightEye = keypoints[1]
            val angle = Math.toDegrees(
                kotlin.math.atan2(
                    (rightEye[1] - leftEye[1]).toDouble(),
                    (rightEye[0] - leftEye[0]).toDouble()
                )
            ).toFloat()

            if (remainingEmojiOptions.isEmpty()) {
                remainingEmojiOptions.addAll(emojiOptions)
            }
            val chosenEmoji = remainingEmojiOptions.random()
            remainingEmojiOptions.remove(chosenEmoji)

            // 构造一个 EmojiDetection 对象，保存该检测的所有信息
            val emojiDetection = EmojiDetection(xCenter, yCenter, diameter, angle, chosenEmoji)
            selectedEmojiList.add(emojiDetection)

            // 绘制单个 emoji
            drawEmoji(canvas, xCenter, yCenter, diameter, angle, chosenEmoji, emojiPaint)
        }

        _selectedEmojis.postValue(selectedEmojiList)
        return mutableBitmap
    }

    /**
     * 当用户修改某个 emoji 时调用：
     * 更新对应的 EmojiDetection 对象，并重新绘制图片
     */
    fun updateEmoji(index: Int, newEmoji: String, newDiameter: Float) {
        val currentList = _selectedEmojis.value?.toMutableList() ?: return
        val updated = currentList[index].copy(emoji = newEmoji, diameter = newDiameter)
        currentList[index] = updated
        _selectedEmojis.postValue(currentList)
        // 根据更新后的 emoji 列表，重新绘制图片
        // 这里假设你保留了原始输入图像 inputBitmap 作为基础（可以在 ViewModel 中存储）
        val baseBitmap = base /* 需要保存原始输入图像 */
        val newBitmap = redrawBitmapWithEmojis(baseBitmap, currentList)
        _outputBitmap.postValue(newBitmap)
    }

    /**
     * 根据传入的 baseBitmap 与当前 EmojiDetection 列表重绘图片
     */
    private fun redrawBitmapWithEmojis(baseBitmap: Bitmap, emojiDetections: List<EmojiDetection>): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        emojiDetections.forEach { ed ->
            drawEmoji(canvas, ed.xCenter, ed.yCenter, ed.diameter, ed.angle, ed.emoji, emojiPaint)
        }
        return mutableBitmap
    }

    /**
     * 在指定的 Canvas 上绘制单个 emoji：
     * - 在 (centerX, centerY) 处绘制，
     * - 使用 diameter 作为文本大小，
     * - 根据 rotationAngle 旋转，
     * - 绘制 emoji 时通过调整 baseline 使文本垂直居中。
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
        paint.textSize = diameter
        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)
        canvas.drawText(emoji, centerX, centerY - (paint.ascent() + paint.descent()) / 2, paint)
        canvas.restore()
    }

    private fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }
}

data class EmojiDetection(
    val xCenter: Float,
    val yCenter: Float,
    val diameter: Float,
    val angle: Float,
    var emoji: String
)

