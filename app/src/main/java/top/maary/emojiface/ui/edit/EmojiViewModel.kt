package top.maary.emojiface.ui.edit

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
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
import top.maary.emojiface.R
import top.maary.emojiface.data.model.datastore.PreferenceRepository
import top.maary.emojiface.facedetection.DetectionResult
import top.maary.emojiface.facedetection.YoloPoseDetector
import top.maary.emojiface.ui.edit.model.EmojiDetection
import top.maary.emojiface.ui.edit.state.ShareEvent
import top.maary.emojiface.util.Constants.DEFAULT_FONT_MARKER
import top.maary.emojiface.util.bitmapToInputStream
import top.maary.emojiface.util.copyUriToInternal
import top.maary.emojiface.util.generateBitmapUri
import top.maary.emojiface.util.getTypeFaceFromPath
import top.maary.emojiface.util.loadFontFromPath
import top.maary.emojiface.util.saveImageToFile
import top.maary.emojiface.util.scaleBitmapIfNeeded
import top.maary.emojiface.util.splitEmoji
import java.io.File
import javax.inject.Inject
import kotlin.math.hypot

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

    private val _font = MutableLiveData<FontFamily?>()
    val font: MutableLiveData<FontFamily?> = _font

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
            _font.value = loadFontFromPath(it)
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
            val componentName =
                ComponentName(application, "${application.packageName}.MainActivityAlias")
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
                detectionResult =
                    faceDetector.detect(bitmapToInputStream(scaledBitmap), ortEnv, ortSession)
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
                val uri = generateBitmapUri(bitmap = bitmap, application = application)
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
            val result = saveImageToFile(bitmap, application)

            if (result != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.save_failed, result),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // 提示成功
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, R.string.save_success, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getRandomEmoji(): String {
        return _emojiList.value!!.random()
    }

    fun copyFontToInternal(uri: Uri) {
        viewModelScope.launch {
            val destFile = copyUriToInternal(uri = uri, application = application)
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
                        refreshResult()
                    }
                }
            }
        }
    }


    // 用户选择 Dropdown 中的字体时调用
    fun onFontSelected(selectedIndex: Int) {
        val font = fontList.value?.getOrNull(selectedIndex) ?: DEFAULT_FONT_MARKER
        viewModelScope.launch {
            preferenceRepository.setSelectedFont(font)
            refreshResult()
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
                canvas,
                it.xCenter,
                it.yCenter,
                it.diameter,
                it.angle,
                it.emoji,
                emojiPaint,
                typeface
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
        val newDetection = EmojiDetection(
            xCenter = x,
            yCenter = y,
            diameter = diameter,
            angle = angle,
            emoji = emoji
        )
        currentList.add(newDetection)
        _selectedEmojis.postValue(currentList)
        val newBitmap = redrawBitmapWithEmojis(base, currentList)
        _outputBitmap.postValue(newBitmap)
    }


    /**
     * 根据传入的 baseBitmap 与当前 EmojiDetection 列表重绘图片
     */
    private fun redrawBitmapWithEmojis(
        baseBitmap: Bitmap,
        emojiDetections: List<EmojiDetection>
    ): Bitmap {
        val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }
        val typeface = getTypeFaceFromPath(_selectedFont.value)

        emojiDetections.forEach { ed ->
            drawEmoji(
                canvas,
                ed.xCenter,
                ed.yCenter,
                ed.diameter,
                ed.angle,
                ed.emoji,
                emojiPaint,
                typeface
            )
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

    override fun onCleared() {
        super.onCleared()
        ortSession.close()
    }
}

