package top.maary.emojiface.facedetection

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import top.maary.emojiface.R
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoloPoseDetector @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Closeable {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var ortSession: OrtSession? = null
    private val modelResId: Int = R.raw.face
    private val inputName: String = "image_bytes" // 确认与模型匹配

    init {
        // ViewModel 中的 Session 创建逻辑移到这里，且只执行一次
        initializeSession()
    }

    private fun initializeSession() {
        try {
            val modelBytes = context.resources.openRawResource(modelResId).readBytes()
            val sessionOptions = OrtSession.SessionOptions().apply {
                registerCustomOpLibrary(OrtxPackage.getLibraryPath())
                // 可选：添加其他优化，如执行提供者 (NNAPI, etc.)
            }
            ortSession = ortEnv.createSession(modelBytes, sessionOptions)
            Log.i("YoloPoseDetector", "ONNX session initialized successfully.")
        } catch (e: Exception) {
            Log.e("YoloPoseDetector", "Failed to initialize ONNX session", e)
            ortSession = null
        }
    }

    /**
     * 使用内部管理的 OrtSession 对输入的图像流进行检测。
     * 输入流应包含 *已经缩放过* 的图片数据，因为模型推理在此数据上进行。
     * 返回的 DetectionResult 包含相对于 *缩放后图片* 的原始坐标。
     *
     * @param inputStream 包含缩放后图片数据的 InputStream。
     * @return 检测结果。
     */
    fun detect(inputStream: InputStream): DetectionResult {
        val currentSession = ortSession
            ?: throw IllegalStateException("ONNX session is not initialized or has been closed.")

        // ViewModel 中关于准备输入 Tensor 的逻辑在这里
        val imageBytes = inputStream.readBytes()
        if (imageBytes.isEmpty()) {
            throw IllegalArgumentException("Input stream was empty.")
        }
        val shape = longArrayOf(imageBytes.size.toLong())
        val byteBuffer = ByteBuffer.wrap(imageBytes)

        // 使用 try-with-resources (`use` block) 确保 Tensor 关闭
        OnnxTensor.createTensor(ortEnv, byteBuffer, shape, OnnxJavaType.UINT8).use { inputTensor ->
            val inputs = mapOf(inputName to inputTensor)

            // ViewModel 中调用 session.run 的逻辑在这里
            currentSession.run(inputs).use { results ->
                // ViewModel 中处理输出 Tensor 的逻辑在这里
                val outputTensor = results[0] as? OnnxTensor
                    ?: throw TypeCastException("Expected OnnxTensor output.")

                val rawDetections = outputTensor.value as Array<FloatArray>
                return DetectionResult(detections = rawDetections)
            }
        }
    }

    override fun close() {
        // ViewModel 中不再需要关心 Session 关闭，这里统一处理
        try {
            ortSession?.close()
            ortSession = null
            Log.i("YoloPoseDetector", "ONNX session closed.")
        } catch (e: Exception) {
            Log.e("YoloPoseDetector", "Error closing ONNX session", e)
        }
    }
}