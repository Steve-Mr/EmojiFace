package top.maary.emojiface.domain.usecase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.graphics.withSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.ui.edit.model.BlurRegion
import top.maary.emojiface.util.createBlurredRegionBitmap
import top.maary.emojiface.util.createHalftoneRegionBitmap
import top.maary.emojiface.util.createPixelatedRegionBitmap
import javax.inject.Inject

class RenderMosaicOnBitmapUseCase @Inject constructor() {

    suspend operator fun invoke(
        baseBitmap: Bitmap,
        regions: List<BlurRegion>,
        blurType: BlurType
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)
            // 不再需要在函数开头初始化 Path
            // val path = Path()

            regions.forEach { region ->
                val blurredRegionBitmap = when (blurType) {
                    is BlurType.Gaussian -> {
                        // 调用统一的工具函数获取模糊小图
                        createBlurredRegionBitmap(baseBitmap, region) // <-- 新的调用
                    }

                    is BlurType.Pixelated -> {
                         createPixelatedRegionBitmap(baseBitmap, region) // 待实现
                        // 临时回退到高斯模糊，以保证编译通过
//                        createBlurredRegionBitmap(baseBitmap, region)
                    }
                    is BlurType.Halftone -> {
                         createHalftoneRegionBitmap(baseBitmap, region) // 待实现
                        // 临时回退到高斯模糊
//                        createBlurredRegionBitmap(baseBitmap, region)
                    }
                }

                // 使用 withSave 保证画布状态的正确保存和恢复
                canvas.withSave {
                    // 1. 以区域中心为轴点旋转画布
                    rotate(region.angle, region.rect.centerX(), region.rect.centerY())

                    // 2. 创建一个椭圆路径用于剪切
                    val ovalPath = Path().apply {
                        addOval(region.rect, Path.Direction.CW)
                    }
                    // 3. 应用剪切路径，这是关键步骤
                    clipPath(ovalPath)

                    // 4. 将模糊小图绘制到被剪切的画布上
                    drawBitmap(blurredRegionBitmap, region.rect.left, region.rect.top, null)
                } // 在这里，画布的旋转和剪切状态被自动恢复

                // 释放中间位图的内存
                blurredRegionBitmap.recycle()
            }
            // 移除函数末尾错误的剪切逻辑

            mutableBitmap
        }
    }
}