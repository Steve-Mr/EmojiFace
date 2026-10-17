package top.maary.emojiface.domain.model.geometry

import android.graphics.RectF

data class RawFaceDetection(
    val boundingBox: RectF,
    val confidence: Float,
    val landmarks: FaceLandmarks,
    val rollAngleDegrees: Float
)
