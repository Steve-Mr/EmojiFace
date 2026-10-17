package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.domain.model.geometry.AnchorTarget
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.geometry.FaceLandmarks
import top.maary.emojiface.domain.model.geometry.Point2D
import top.maary.emojiface.domain.model.geometry.RawFaceDetection
import top.maary.emojiface.domain.model.geometry.Size2D
import top.maary.emojiface.facedetection.YoloPoseDetector
import top.maary.emojiface.util.bitmapToInputStream
import top.maary.emojiface.util.scaleBitmapIfNeeded
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class FaceDetectionResult(
    val geometries: List<FaceGeometry>,
    val rawDetections: List<RawFaceDetection>,
    val detectionScaleX: Float,
    val detectionScaleY: Float
)

class DetectFaceGeometriesUseCase @Inject constructor(
    private val faceDetector: YoloPoseDetector
) {
    suspend operator fun invoke(bitmapForDetection: Bitmap): Result<FaceDetectionResult> = withContext(Dispatchers.Default) {
        runCatching {
            // 1. Scale image if needed for efficient inference
            val scaledBitmap = scaleBitmapIfNeeded(bitmapForDetection)

            // 2. Scale factors to map from detector coordinates to original image coordinates
            val detectionScaleX = bitmapForDetection.width.toFloat() / scaledBitmap.width.toFloat()
            val detectionScaleY = bitmapForDetection.height.toFloat() / scaledBitmap.height.toFloat()

            // 3. Run YOLO pose face detector
            val detectionResult = withContext(Dispatchers.IO) {
                faceDetector.detect(bitmapToInputStream(scaledBitmap))
            }
            if (scaledBitmap !== bitmapForDetection) {
                scaledBitmap.recycle()
            }

            // 4. Sort detections from left to right (by x center)
            val sortedDetections = detectionResult.detections.sortedBy { it[0] }

            val rawDetections = ArrayList<RawFaceDetection>(sortedDetections.size)
            val geometries = ArrayList<FaceGeometry>(sortedDetections.size)

            for (detection in sortedDetections) {
                val xCenter = detection[0] * detectionScaleX
                val yCenter = detection[1] * detectionScaleY
                val width = detection[2] * detectionScaleX
                val height = detection[3] * detectionScaleY
                val confidence = if (detection.size > 4) detection[4] else 1.0f

                val boundingBox = RectF(
                    xCenter - width / 2f,
                    yCenter - height / 2f,
                    xCenter + width / 2f,
                    yCenter + height / 2f
                )

                // Keypoints: 5 facial landmarks (leftEye, rightEye, noseTip, leftMouth, rightMouth)
                val leftEye = Point2D(
                    x = detection[6] * detectionScaleX,
                    y = detection[7] * detectionScaleY
                )
                val rightEye = Point2D(
                    x = detection[9] * detectionScaleX,
                    y = detection[10] * detectionScaleY
                )
                val noseTip = Point2D(
                    x = detection[12] * detectionScaleX,
                    y = detection[13] * detectionScaleY
                )
                val leftMouthCorner = Point2D(
                    x = detection[15] * detectionScaleX,
                    y = detection[16] * detectionScaleY
                )
                val rightMouthCorner = Point2D(
                    x = detection[18] * detectionScaleX,
                    y = detection[19] * detectionScaleY
                )

                val landmarks = FaceLandmarks(
                    leftEye = leftEye,
                    rightEye = rightEye,
                    noseTip = noseTip,
                    leftMouthCorner = leftMouthCorner,
                    rightMouthCorner = rightMouthCorner
                )

                val rollAngleDegrees = Math.toDegrees(
                    atan2(
                        (rightEye.y - leftEye.y).toDouble(),
                        (rightEye.x - leftEye.x).toDouble()
                    )
                ).toFloat()

                val rawDetection = RawFaceDetection(
                    boundingBox = boundingBox,
                    confidence = confidence,
                    landmarks = landmarks,
                    rollAngleDegrees = rollAngleDegrees
                )
                rawDetections.add(rawDetection)

                // Compute canonical FaceGeometry (diameter adjusted for aspect ratio, center, roll angle)
                val diagonal = hypot(width.toDouble(), height.toDouble()).toFloat()
                val maxDim = max(width, height)
                val diffRatio = if (maxDim > 0f) abs(width - height) / maxDim else 0f
                val diameter = width * (1f - diffRatio) + diagonal * diffRatio

                val geometry = FaceGeometry(
                    center = Point2D(xCenter, yCenter),
                    baseSize = Size2D(diameter, diameter),
                    scaleFactor = 1.0f,
                    rotationDegrees = rollAngleDegrees,
                    anchorTarget = AnchorTarget.FACE
                )
                geometries.add(geometry)
            }

            FaceDetectionResult(
                geometries = geometries,
                rawDetections = rawDetections,
                detectionScaleX = detectionScaleX,
                detectionScaleY = detectionScaleY
            )
        }
    }
}
