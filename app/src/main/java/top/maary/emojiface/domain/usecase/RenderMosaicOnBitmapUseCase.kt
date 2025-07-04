package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.util.createBlurredRegionBitmap
import javax.inject.Inject

class RenderMosaicOnBitmapUseCase @Inject constructor() {

    suspend operator fun invoke(
        baseBitmap: Bitmap,
        regions: List<RectF>,
        blurType: BlurType
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            regions.forEach { regionRectF ->
                when (blurType) {
                    is BlurType.Gaussian -> {
                        // 调用统一的工具函数
                        val blurredRegionBitmap = createBlurredRegionBitmap(baseBitmap, regionRectF)

                        // 将处理好的模糊小图绘制到最终画布的正确位置
                        canvas.drawBitmap(blurredRegionBitmap, regionRectF.left, regionRectF.top, null)

                        // 释放内存
                        blurredRegionBitmap.recycle()
                    }
                    // 在此处理未来可能的新模糊类型
                }
            }
            mutableBitmap
        }
    }
}