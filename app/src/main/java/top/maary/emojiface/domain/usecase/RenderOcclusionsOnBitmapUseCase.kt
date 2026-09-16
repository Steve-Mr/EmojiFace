package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.domain.model.occlusion.FaceOcclusion
import top.maary.emojiface.domain.rendering.RenderContext
import top.maary.emojiface.domain.rendering.UnifiedOcclusionRenderer
import javax.inject.Inject

class RenderOcclusionsOnBitmapUseCase @Inject constructor(
    private val occlusionRenderer: UnifiedOcclusionRenderer
) {
    suspend operator fun invoke(
        baseBitmap: Bitmap,
        occlusions: List<FaceOcclusion>,
        loadedTypefaces: Map<String, Typeface> = emptyMap(),
        cachedAvatarBitmaps: Map<String, Bitmap> = emptyMap(),
        defaultTypeface: Typeface = Typeface.DEFAULT
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            val renderContext = RenderContext(
                baseBitmap = baseBitmap,
                coordinateScale = 1.0f,
                defaultTypeface = defaultTypeface,
                loadedTypefaces = loadedTypefaces,
                cachedAvatarBitmaps = cachedAvatarBitmaps
            )
            occlusionRenderer.render(canvas, occlusions, renderContext)
            mutableBitmap
        }
    }
}
