package top.maary.emojiface.domain.model.geometry

data class FaceLandmarks(
    val leftEye: Point2D,
    val rightEye: Point2D,
    val noseTip: Point2D,
    val leftMouthCorner: Point2D,
    val rightMouthCorner: Point2D
) {
    val eyeDistance: Float get() = leftEye.distanceTo(rightEye)

    val eyeCenter: Point2D get() = Point2D(
        x = (leftEye.x + rightEye.x) / 2f,
        y = (leftEye.y + rightEye.y) / 2f
    )

    val mouthCenter: Point2D get() = Point2D(
        x = (leftMouthCorner.x + rightMouthCorner.x) / 2f,
        y = (leftMouthCorner.y + rightMouthCorner.y) / 2f
    )
}
