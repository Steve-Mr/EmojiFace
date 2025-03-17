package top.maary.emojiface

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Collections

// 用于存放检测结果的 data class， detections 中的每个 FloatArray 表示一个检测结果，包含边界框、置信度和关键点信息
data class DetectionResult( val detections: Array<FloatArray> )

internal class YoloPoseDetector {
    /**
     * 使用传入的 InputStream（例如来自文件或资源）加载图片，
     * 并调用 ONNX 模型进行推理，返回原始的检测数据。
     */
    fun detect(inputStream: InputStream, ortEnv: OrtEnvironment, ortSession: OrtSession ): DetectionResult {
    // Step 1: 读取图片的原始字节数据
    val imageBytes = inputStream.readBytes()
    // Step 2: 根据字节数据构造 ONNX 模型所需的 tensor
    // 注意：模型内部已实现预处理，这里直接传入原始图片数据即可
    val shape = longArrayOf(imageBytes.size.toLong())
    val byteBuffer = ByteBuffer.wrap(imageBytes)
    val inputTensor = OnnxTensor.createTensor(ortEnv, byteBuffer, shape, OnnxJavaType.UINT8)

    inputTensor.use { tensor ->
        // Step 3: 运行推理，输入名称应与模型定义一致（此处为 "image_bytes"）
        val inputMap = mapOf("image_bytes" to tensor)
        // 这里直接调用 run，默认返回所有输出张量
        val output = ortSession.run(inputMap)
        output.use { results ->
            // 假设模型输出的第一个 tensor 为检测结果，格式为 (num_boxes, 21)
            // 每个检测的前 6 个数依次为 [x_center, y_center, width, height, confidence, class_id]
            // 后续的 15 个数为 5 个关键点，每个关键点由 [x, y, confidence] 构成
            val rawDetections = results[0].value as Array<FloatArray>
            return DetectionResult(rawDetections)
        }
    }
    }
}
