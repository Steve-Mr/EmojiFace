package top.maary.emojiface.facedetection

// 用于存放检测结果的 data class， detections 中的每个 FloatArray 表示一个检测结果，包含边界框、置信度和关键点信息
data class DetectionResult( val detections: Array<FloatArray> ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectionResult

        return detections.contentDeepEquals(other.detections)
    }

    override fun hashCode(): Int {
        return detections.contentDeepHashCode()
    }
}