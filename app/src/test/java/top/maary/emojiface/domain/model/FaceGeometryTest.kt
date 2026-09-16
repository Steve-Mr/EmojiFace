package top.maary.emojiface.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.maary.emojiface.domain.model.geometry.AnchorTarget
import top.maary.emojiface.domain.model.geometry.FaceGeometry
import top.maary.emojiface.domain.model.geometry.FaceLandmarks
import top.maary.emojiface.domain.model.geometry.Point2D
import top.maary.emojiface.domain.model.geometry.Size2D
import kotlin.math.cos
import kotlin.math.sin

class FaceGeometryTest {

    @Test
    fun contains_at0Degrees_ellipseAndBox() {
        val geometry = FaceGeometry(
            center = Point2D(100f, 100f),
            baseSize = Size2D(100f, 100f),
            scaleFactor = 1.0f,
            rotationDegrees = 0f
        )
        // Center point
        assertTrue(geometry.contains(Point2D(100f, 100f), treatAsEllipse = true))
        assertTrue(geometry.contains(Point2D(100f, 100f), treatAsEllipse = false))

        // On ellipse perimeter (right and top)
        assertTrue(geometry.contains(Point2D(150f, 100f), treatAsEllipse = true))
        assertTrue(geometry.contains(Point2D(100f, 50f), treatAsEllipse = true))

        // Outside ellipse perimeter
        assertFalse(geometry.contains(Point2D(151f, 100f), treatAsEllipse = true))
        assertFalse(geometry.contains(Point2D(100f, 49f), treatAsEllipse = true))

        // Corner point: (140, 140) -> offset (40, 40)
        // Ellipse: 40^2/50^2 + 40^2/50^2 = 3200/2500 = 1.28 > 1 -> outside
        assertFalse(geometry.contains(Point2D(140f, 140f), treatAsEllipse = true))
        // Box: |40| <= 50 and |40| <= 50 -> inside
        assertTrue(geometry.contains(Point2D(140f, 140f), treatAsEllipse = false))

        // Far outside
        assertFalse(geometry.contains(Point2D(200f, 200f), treatAsEllipse = false))
    }

    @Test
    fun contains_at90Degrees_rotatedAxes() {
        // Non-square: width=200 (halfW=100), height=100 (halfH=50), rotated 90 degrees clockwise.
        // Major axis (originally horizontal) now aligns with +Y axis.
        // Minor axis (originally vertical) now aligns with -X axis.
        val geometry = FaceGeometry(
            center = Point2D(100f, 100f),
            baseSize = Size2D(200f, 100f),
            scaleFactor = 1.0f,
            rotationDegrees = 90f
        )

        // Point offset along +Y (which is the rotated major axis): (100, 180), offset = 80 <= 100
        assertTrue(geometry.contains(Point2D(100f, 180f), treatAsEllipse = true))
        // Point offset along -Y: (100, 15), offset = 85 <= 100
        assertTrue(geometry.contains(Point2D(100f, 15f), treatAsEllipse = true))
        // Point beyond major axis: (100, 210), offset = 110 > 100
        assertFalse(geometry.contains(Point2D(100f, 210f), treatAsEllipse = true))

        // Point offset along +X (minor axis): (180, 100), offset = 80 > halfH(50)
        assertFalse(geometry.contains(Point2D(180f, 100f), treatAsEllipse = true))
        // Point within minor axis: (140, 100), offset = 40 <= halfH(50)
        assertTrue(geometry.contains(Point2D(140f, 100f), treatAsEllipse = true))
    }

    @Test
    fun contains_at45Degrees_diagonalHitTest() {
        // baseSize: width=200 (halfW=100), height=100 (halfH=50), rotated 45 degrees clockwise.
        val geometry = FaceGeometry(
            center = Point2D(100f, 100f),
            baseSize = Size2D(200f, 100f),
            scaleFactor = 1.0f,
            rotationDegrees = 45f
        )

        val rad45 = Math.toRadians(45.0)
        val cos45 = cos(rad45).toFloat()
        val sin45 = sin(rad45).toFloat()

        // Point 70 units along the 45-degree major axis
        val majorPoint = Point2D(100f + 70f * cos45, 100f + 70f * sin45)
        assertTrue(geometry.contains(majorPoint, treatAsEllipse = true))

        // Point 110 units along the 45-degree major axis (exceeds halfW = 100)
        val outsideMajorPoint = Point2D(100f + 110f * cos45, 100f + 110f * sin45)
        assertFalse(geometry.contains(outsideMajorPoint, treatAsEllipse = true))

        // Point 70 units perpendicular to the major axis (along -45 degrees)
        // Since halfH = 50, 70 units exceeds the minor axis
        val perpPoint = Point2D(100f + 70f * sin45, 100f - 70f * cos45)
        assertFalse(geometry.contains(perpPoint, treatAsEllipse = true))

        // Point 35 units perpendicular to the major axis (within halfH = 50)
        val insidePerpPoint = Point2D(100f + 35f * sin45, 100f - 35f * cos45)
        assertTrue(geometry.contains(insidePerpPoint, treatAsEllipse = true))
    }

    @Test
    fun contains_scaleFactorVariations() {
        val baseGeometry = FaceGeometry(
            center = Point2D(100f, 100f),
            baseSize = Size2D(100f, 100f),
            scaleFactor = 1.0f,
            rotationDegrees = 0f
        )
        val testPoint = Point2D(180f, 100f) // offset 80 from center

        // At 1.0x (halfW = 50), point is outside
        assertFalse(baseGeometry.contains(testPoint, treatAsEllipse = true))

        // At 2.0x (halfW = 100), point is inside
        val scaledUp = baseGeometry.copy(scaleFactor = 2.0f)
        assertTrue(scaledUp.contains(testPoint, treatAsEllipse = true))

        // At 0.5x (halfW = 25), closer point (130, 100) is outside
        val scaledDown = baseGeometry.copy(scaleFactor = 0.5f)
        assertFalse(scaledDown.contains(Point2D(130f, 100f), treatAsEllipse = true))
        assertTrue(baseGeometry.contains(Point2D(130f, 100f), treatAsEllipse = true))
    }

    @Test
    fun contains_aspectRatioVariations() {
        // Wide aspect ratio: 300x100
        val wideGeometry = FaceGeometry(
            center = Point2D(0f, 0f),
            baseSize = Size2D(300f, 100f),
            scaleFactor = 1.0f,
            rotationDegrees = 0f
        )
        assertTrue(wideGeometry.contains(Point2D(120f, 0f), treatAsEllipse = true))
        assertFalse(wideGeometry.contains(Point2D(0f, 60f), treatAsEllipse = true))

        // Tall aspect ratio: 100x300
        val tallGeometry = FaceGeometry(
            center = Point2D(0f, 0f),
            baseSize = Size2D(100f, 300f),
            scaleFactor = 1.0f,
            rotationDegrees = 0f
        )
        assertFalse(tallGeometry.contains(Point2D(120f, 0f), treatAsEllipse = true))
        assertTrue(tallGeometry.contains(Point2D(0f, 120f), treatAsEllipse = true))
    }

    @Test
    fun transform_scalesCenterAndBaseSize() {
        val original = FaceGeometry(
            center = Point2D(50f, 80f),
            baseSize = Size2D(100f, 120f),
            scaleFactor = 1.5f,
            rotationDegrees = 30f,
            anchorTarget = AnchorTarget.EYES
        )
        val transformed = original.transform(2.5f)

        assertEquals(125f, transformed.center.x, 0.001f)
        assertEquals(200f, transformed.center.y, 0.001f)
        assertEquals(250f, transformed.baseSize.width, 0.001f)
        assertEquals(300f, transformed.baseSize.height, 0.001f)
        assertEquals(1.5f, transformed.scaleFactor, 0.001f)
        assertEquals(30f, transformed.rotationDegrees, 0.001f)
        assertEquals(AnchorTarget.EYES, transformed.anchorTarget)
    }

    @Test
    fun point2D_vectorOperations() {
        val p1 = Point2D(10f, 20f)
        val p2 = Point2D(40f, 60f)

        assertEquals(50f, p1.distanceTo(p2), 0.001f)
        assertEquals(Point2D(50f, 80f), p1 + p2)
        assertEquals(Point2D(30f, 40f), p2 - p1)
    }

    @Test
    fun size2D_operations() {
        val size = Size2D(200f, 100f)

        assertEquals(200f, size.maxDimension, 0.001f)
        assertEquals(100f, size.minDimension, 0.001f)
        assertEquals(Size2D(400f, 200f), size.scale(2.0f))
    }

    @Test
    fun faceLandmarks_properties() {
        val landmarks = FaceLandmarks(
            leftEye = Point2D(20f, 50f),
            rightEye = Point2D(80f, 50f),
            noseTip = Point2D(50f, 75f),
            leftMouthCorner = Point2D(30f, 90f),
            rightMouthCorner = Point2D(70f, 90f)
        )

        assertEquals(60f, landmarks.eyeDistance, 0.001f)
        assertEquals(Point2D(50f, 50f), landmarks.eyeCenter)
        assertEquals(Point2D(50f, 90f), landmarks.mouthCenter)
    }
}
