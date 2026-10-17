package top.maary.emojiface.domain.rendering

import android.graphics.Bitmap
import android.graphics.Typeface

data class RenderContext(
    val baseBitmap: Bitmap,
    val coordinateScale: Float = 1.0f,
    val defaultTypeface: Typeface = Typeface.DEFAULT,
    val loadedTypefaces: Map<String, Typeface> = emptyMap(),
    val cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
    val isSlidingPreview: Boolean = false,
    val editingOcclusionId: Long? = null
)
