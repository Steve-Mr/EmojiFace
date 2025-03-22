package top.maary.emojiface

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.maary.emojiface.Constants.DEFAULT_FONT_MARKER
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.text.BreakIterator
import java.util.Locale
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.random.Random

@HiltViewModel
class EmojiViewModel @Inject constructor(
    @ApplicationContext private val application: Context,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private lateinit var model: ByteArray
    private val modelId = R.raw.face
    private val faceDetector = YoloPoseDetector()
    // 暂存检测结果，供后续多次调用 processDetections 使用
    private var detectionResult: DetectionResult? = null

    private val _emojiList = MutableLiveData<List<String>>()
    val emojiList: MutableLiveData<List<String>> = _emojiList

    private val _iconHideState = MutableLiveData<Boolean>()
    val iconHideState: MutableLiveData<Boolean> = _iconHideState

    private val _fontList = MutableLiveData<List<String>>()
    val fontList: MutableLiveData<List<String>> = _fontList

    private val _selectedFont = MutableLiveData<String>()
    val selectedFont: MutableLiveData<String> = _selectedFont

    private val _font = MutableLiveData<FontFamily>()
    val font: MutableLiveData<FontFamily> = _font

    init {
        preferenceRepository.emojiOptionsFlow.onEach {
            _emojiList.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.isIconHide.onEach {
            _iconHideState.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.fontsList.onEach {
            _fontList.value = it
        }.launchIn(viewModelScope)
        preferenceRepository.selectedFont.onEach {
            _selectedFont.value = it
            _font.value = this.loadFontFromPath(it)
        }.launchIn(viewModelScope)
    }

    // LiveData 用于将处理后的 Bitmap 传递给 UI 层显示
    private val _outputBitmap = MutableLiveData<Bitmap?>()
    val outputBitmap: MutableLiveData<Bitmap?> = _outputBitmap

    // LiveData 保存每个检测目标对应的选取的 emoji 顺序
    private val _selectedEmojis = MutableLiveData<List<EmojiDetection>>()
    val selectedEmojis: LiveData<List<EmojiDetection>> = _selectedEmojis

    private lateinit var base: Bitmap

    // 添加图片状态
    private val _currentImage = MutableLiveData<Bitmap?>(null)
    val currentImage: LiveData<Bitmap?> = _currentImage

    // 在 EmojiViewModel 中添加以下变量
    private var scaleFactorX: Float = 1.0f
    private var scaleFactorY: Float = 1.0f

    // 在 EmojiViewModel.kt 中添加
    sealed class ShareEvent {
        data class ShareImage(val uri: Uri) : ShareEvent()
        data class Error(val message: String) : ShareEvent()
    }

    private val _shareEvent = MutableSharedFlow<ShareEvent>()
    val shareEvent: SharedFlow<ShareEvent> = _shareEvent.asSharedFlow()

    // 清空图片方法
    fun clearImage() {
        _currentImage.postValue(null)
        _outputBitmap.postValue(null)
        _selectedEmojis.postValue(emptyList())
    }

    fun updateEmojiList(emojis: String) {
        if (emojis.isEmpty()) {
            resetEmojiList()
            return
        }
        viewModelScope.launch {
            // 使用 splitEmoji 来确保正确拆分复合 emoji
            val emojiList = splitEmoji(emojis)
            preferenceRepository.updateEmojiOptions(emojiList)
        }
    }

    fun toggleLauncherIcon(hideIcon: Boolean) {
        viewModelScope.launch {
            val packageManager = application.packageManager
            val componentName = ComponentName(application, "${application.packageName}.MainActivityAlias")
            val newState = if (hideIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
            preferenceRepository.updateIconState(hideIcon)
        }
    }



    private fun resetEmojiList() {
        viewModelScope.launch {
            preferenceRepository.updateEmojiOptions(PreferenceRepository.DEFAULT_EMOJI_LIST)
        }
    }

    /**
     * 调用模型进行检测，并暂存检测结果。
     */
    fun detect(inputUri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            clearImage()  // 先清除旧数据
            application.contentResolver.openInputStream(inputUri)?.use { stream ->
                val input = BitmapFactory.decodeStream(stream)
                val scaledBitmap = scaleBitmapIfNeeded(input)
                scaleFactorX = input.width.toFloat() / scaledBitmap.width.toFloat()
                scaleFactorY = input.height.toFloat() / scaledBitmap.height.toFloat()

                _currentImage.postValue(input)
                base = input // 保存原图

                val sessionOptions = OrtSession.SessionOptions().apply {
                    registerCustomOpLibrary(OrtxPackage.getLibraryPath())
                }
                model = application.resources.openRawResource(modelId).readBytes()
                ortSession = ortEnv.createSession(model, sessionOptions)

                // 使用缩放后的图片进行检测
                detectionResult = faceDetector.detect(bitmapToInputStream(scaledBitmap), ortEnv, ortSession)
                // 处理检测结果，传入原图
                val processedBitmap = processDetections()
                _outputBitmap.postValue(processedBitmap)
            }
        }
    }

    // 修改后的 shareImage 函数
    fun shareImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 保存到缓存文件
                val cachePath = File(application.cacheDir, "images").apply { mkdirs() }
                val file = File(cachePath, "shared_${System.currentTimeMillis()}.png").apply {
                    FileOutputStream(this).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                }

                // 生成安全 Uri
                val uri = FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    file
                )

                // 发送分享事件
                _shareEvent.emit(ShareEvent.ShareImage(uri))
            } catch (e: Exception) {
                _shareEvent.emit(ShareEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    // 工具函数：保存图片到相册
    fun saveImageToGallery(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 使用 MediaStore API 保存到公共目录
                val folderName = "FaceMoji"
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "facemoji_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH,  "${Environment.DIRECTORY_PICTURES}/$folderName")
                }

                // 插入 MediaStore 并获取 Uri
                val resolver = application.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("cannot create file")

                // 写入图片数据
                resolver.openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IOException("failed")
                    }
                }

                // 提示成功
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, R.string.save_success, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, application.getString(R.string.save_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getRandomEmoji(): String {
        return _emojiList.value!!.random()
    }

    fun copyFontToInternal(uri: Uri) {
        viewModelScope.launch {
            val contentResolver = application.contentResolver

            // 从 URI 中获取文件名
            val originalFileName = getFileNameFromUri(uri)

            // 提取扩展名（包括点）
            var extension = originalFileName.substringAfterLast('.', "")
            val fileNameWithoutExtension = originalFileName.substringBeforeLast('.')
            if (extension.isNotEmpty()) {
                extension = ".$extension"
            }

            // 定义支持的字体扩展名
            val supportedExtensions = listOf(".ttf", ".otf")
            // 如果提取不到有效扩展名，则根据 MIME 类型推断；否则，若不支持则可选择默认扩展名或拒绝处理
            if (extension.isEmpty() || extension !in supportedExtensions) {
                extension = ".ttf"
            }

            val fileName = "${fileNameWithoutExtension}_${generateShortUniqueId()}$extension"
            val destFile = File(application.filesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 复制完成后，将新字体路径存入 Preference 或其他存储方案中
            preferenceRepository.addFont(destFile.absolutePath)
        }
    }

    fun removeFontFromInternal(filePath: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val file = File(filePath)
                if (file.exists()) {
                    if (filePath == _selectedFont.value) {
                        preferenceRepository.setSelectedFont(DEFAULT_FONT_MARKER)
                    }
                    val deleted = file.delete()
                    if (deleted) {
                        preferenceRepository.removeFont(filePath)
                    }
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        application.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                cursor.moveToFirst()
                return cursor.getString(nameIndex)
            }
        }
        return ""
    }

    private fun generateShortUniqueId(): String {
        val timestamp = System.currentTimeMillis() / 1000 // 秒级时间戳
        val random = Random.nextInt(1000) // 0-999 随机数
        return String.format(Locale.getDefault().toString(), timestamp % 1000, random) // 格式化为 6 位数字
    }

    // 用户选择 Dropdown 中的字体时调用
    fun onFontSelected(selectedIndex: Int) {
        val font = fontList.value?.getOrNull(selectedIndex) ?: DEFAULT_FONT_MARKER
        viewModelScope.launch {
            preferenceRepository.setSelectedFont(font)
            refreshResult()
        }
    }

    private fun loadFontFromPath(filePath: String?): FontFamily? {
        if (filePath.isNullOrEmpty()) return null
        val file = File(filePath)
        if (file.exists()) {
            try {
                return FontFamily(Font(file = file))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return null
    }

    private fun getTypeFaceFromPath(filePath: String?): Typeface? {
        if (filePath.isNullOrEmpty()) return null
        val file = File(filePath)
        if (file.exists()) {
            try {
                return Typeface.createFromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return null
    }

    fun getFileNameWithoutExtensionUsingPath(filePath: String): String {
        val path = Paths.get(filePath)
        val fileName = path.fileName.toString()
        val dotIndex = fileName.lastIndexOf(".")
        return if (dotIndex == -1) {
            fileName
        } else {
            fileName.substring(0, dotIndex)
        }
    }


    /**
     * 对传入的 Bitmap 根据检测结果绘制 emoji，并构造 EmojiDetection 列表
     */
    private fun processDetections(): Bitmap {
        val mutableBitmap = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val selectedEmojiList = mutableListOf<EmojiDetection>()
        val sortedDetections = detectionResult?.detections?.sortedBy { it[0] } ?: emptyList()
        val remainingEmojiOptions = _emojiList.value!!.toMutableList()

        sortedDetections.forEach { detection ->
            // 转换坐标到原图尺寸
            val xCenter = detection[0] * scaleFactorX
            val yCenter = detection[1] * scaleFactorY
            val width = detection[2] * scaleFactorX
            val height = detection[3] * scaleFactorY
            val diagonal = hypot(width.toDouble(), height.toDouble()).toFloat()
            val diffRatio = kotlin.math.abs(width - height) / kotlin.math.max(width, height)
            val diameter = width * (1 - diffRatio) + diagonal * diffRatio

            // 处理关键点坐标
            val keypoints = Array(5) { FloatArray(3) }
            for (i in 0 until 5) {
                keypoints[i][0] = detection[6 + i * 3] * scaleFactorX
                keypoints[i][1] = detection[6 + i * 3 + 1] * scaleFactorY
                keypoints[i][2] = detection[6 + i * 3 + 2]
            }
            val leftEye = keypoints[0]
            val rightEye = keypoints[1]
            val angle = Math.toDegrees(
                kotlin.math.atan2(
                    (rightEye[1] - leftEye[1]).toDouble(),
                    (rightEye[0] - leftEye[0]).toDouble()
                )
            ).toFloat()

            if (remainingEmojiOptions.isEmpty()) {
                _emojiList.value?.let { remainingEmojiOptions.addAll(it) }
            }
            val chosenEmoji = remainingEmojiOptions.random()
            remainingEmojiOptions.remove(chosenEmoji)

            val emojiDetection = EmojiDetection(xCenter, yCenter, diameter, angle, chosenEmoji)
            selectedEmojiList.add(emojiDetection)

            val typeface = getTypeFaceFromPath(_selectedFont.value)

            drawEmoji(canvas, xCenter, yCenter, diameter, angle, chosenEmoji, emojiPaint, typeface)
        }

        _selectedEmojis.postValue(selectedEmojiList)
        return mutableBitmap
    }

    private fun refreshResult() {
        val mutableBitmap = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val typeface = getTypeFaceFromPath(_selectedFont.value)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        _selectedEmojis.value?.forEach {
            drawEmoji(
                canvas, it.xCenter, it.yCenter, it.diameter, it.angle, it.emoji, emojiPaint, typeface
            )
        }
        _outputBitmap.value = mutableBitmap
    }

    /**
     * 当用户修改某个 emoji 时调用：
     * 更新对应的 EmojiDetection 对象，并重新绘制图片
     */
    fun updateEmoji(index: Int, newEmoji: String, newDiameter: Float, newAngle: Float) {
        val currentList = _selectedEmojis.value?.toMutableList() ?: return
        if (newEmoji == "") {
            currentList.removeAt(index)
        } else {
            val updated =
                currentList[index].copy(emoji = newEmoji, diameter = newDiameter, angle = newAngle)
            currentList[index] = updated
        }
        _selectedEmojis.postValue(currentList)
        // 根据更新后的 emoji 列表，重新绘制图片
        // 这里假设你保留了原始输入图像 inputBitmap 作为基础（可以在 ViewModel 中存储）
        val baseBitmap = base /* 需要保存原始输入图像 */
        val newBitmap = redrawBitmapWithEmojis(baseBitmap, currentList)
        _outputBitmap.postValue(newBitmap)
    }

    fun addEmoji(x: Float, y: Float, emoji: String, diameter: Float, angle: Float) {
        val currentList = _selectedEmojis.value?.toMutableList() ?: mutableListOf()
        // 默认角度设置为 0
        val newDetection = EmojiDetection(xCenter = x, yCenter = y, diameter = diameter, angle = angle, emoji = emoji)
        currentList.add(newDetection)
        _selectedEmojis.postValue(currentList)
        val newBitmap = redrawBitmapWithEmojis(base, currentList)
        _outputBitmap.postValue(newBitmap)
    }


    /**
     * 根据传入的 baseBitmap 与当前 EmojiDetection 列表重绘图片
     */
    private fun redrawBitmapWithEmojis(baseBitmap: Bitmap, emojiDetections: List<EmojiDetection>): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val typeface = getTypeFaceFromPath(_selectedFont.value)

        emojiDetections.forEach { ed ->
            drawEmoji(canvas, ed.xCenter, ed.yCenter, ed.diameter, ed.angle, ed.emoji, emojiPaint, typeface)
        }
        return mutableBitmap
    }

    /**
     * 在指定的 Canvas 上绘制单个 emoji：
     * - 在 (centerX, centerY) 处绘制，
     * - 使用 diameter 作为文本大小，
     * - 根据 rotationAngle 旋转，
     * - 绘制 emoji 时通过调整 baseline 使文本垂直居中。
     */
    private fun drawEmoji(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        diameter: Float,
        rotationAngle: Float,
        emoji: String,
        paint: Paint,
        typeface: Typeface?
    ) {
        paint.textSize = diameter
        paint.setTypeface(typeface)
        canvas.save()
        canvas.rotate(rotationAngle, centerX, centerY)
        canvas.drawText(emoji, centerX, centerY - (paint.ascent() + paint.descent()) / 2, paint)
        canvas.restore()
    }

    // 添加缩放函数
    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxSize = 1024 // 设置最大边长阈值
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scaleFactor = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }

        val scaledWidth = (width * scaleFactor).toInt()
        val scaledHeight = (height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    private fun bitmapToInputStream(bitmap: Bitmap?): InputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private fun splitEmoji(text: String): List<String> {
        val breaker = BreakIterator.getCharacterInstance(Locale.getDefault())
        breaker.setText(text)
        val result = mutableListOf<String>()
        var start = breaker.first()
        var end = breaker.next()
        while (end != BreakIterator.DONE) {
            result.add(text.substring(start, end))
            start = end
            end = breaker.next()
        }
        return result
    }
}

data class EmojiDetection(
    val xCenter: Float,
    val yCenter: Float,
    val diameter: Float,
    val angle: Float,
    var emoji: String
)

