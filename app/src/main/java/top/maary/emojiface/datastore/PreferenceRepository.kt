package top.maary.emojiface.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import top.maary.emojiface.BuildConfig
import top.maary.emojiface.ui.edit.model.EmojiList
import top.maary.emojiface.util.Constants.DEFAULT_FONT_MARKER
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "emoji_settings",
)

class PreferenceRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore
    companion object {
        val EMOJI_LIST = stringPreferencesKey("emoji_options")
        // 定义默认的 emoji 列表
        val DEFAULT_EMOJI_LIST = listOf("😂", "😎", "😆", "😋", "🫡", "😊", "😜", "🤠")
        val IS_ICON_HIDE = booleanPreferencesKey("hide_app_icon")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
        val FONT_LIST = stringPreferencesKey("font_list")
        val EASTER_EGG = booleanPreferencesKey("easter_egg_enabled")
        val IS_TOO_DEEP = booleanPreferencesKey("is_too_deep")
        val MOSAIC_MODE = intPreferencesKey("mosaic_mode")
        const val MOSAIC_MODE_EMOJI = 0
        const val MOSAIC_MODE_BLUR = 1
        val MOSAIC_TYPE = intPreferencesKey("mosaic_type")
        const val MOSAIC_TYPE_GAUSSIAN = 0  // 高斯模糊
        const val MOSAIC_TYPE_PIXELATED = 1 // 像素化
        const val MOSAIC_TYPE_HALFTONE = 2  // 半色调网点效果
        val MOSAIC_TARGET = intPreferencesKey("mosaic_target")
        const val MOSAIC_TARGET_FACE = 0 // 作用于整个面部
        const val MOSAIC_TARGET_EYES = 1 // 仅作用于眼部
    }

    // 从 DataStore 中读取 emoji 列表（以逗号分隔存储）
    val emojiOptionsFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[EMOJI_LIST]?.let { jsonString ->
            // 反序列化，如果失败则返回 null
            runCatching { Json.decodeFromString<EmojiList>(jsonString) }.getOrNull()?.emojis
        } ?: DEFAULT_EMOJI_LIST
    }.distinctUntilChanged()

    // 更新 DataStore 中的 emoji 列表
    suspend fun updateEmojiOptions(newOptions: List<String>) {
        val emojiList = EmojiList(newOptions)
        val jsonString = Json.encodeToString(emojiList)
        dataStore.edit { preferences ->
            preferences[EMOJI_LIST] = jsonString
        }
    }

    val isIconHide: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_ICON_HIDE] ?: !BuildConfig.ICON_ENABLED
    }.distinctUntilChanged()

    suspend fun updateIconState(state: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ICON_HIDE] = state
        }
    }

    // 添加字体，若列表中不存在则添加
    suspend fun addFont(fontPath: String) {
        dataStore.edit { prefs ->
            val currentFonts = prefs[FONT_LIST]?.split(",")?.toMutableList() ?: mutableListOf()
            if (fontPath !in currentFonts) {
                currentFonts.add(fontPath)
            }
            prefs[FONT_LIST] = currentFonts.joinToString(",")
        }
    }

    suspend fun removeFont(fontPath: String) {
        dataStore.edit { prefs ->
            val currentFonts = prefs[FONT_LIST]?.split(",")?.toMutableList() ?: mutableListOf()
            if (fontPath in currentFonts) {
                currentFonts.remove(fontPath)
            }
            prefs[FONT_LIST] = currentFonts.joinToString(",")
        }
    }

    // 设置用户选择的字体。默认值用 FontConstants.DEFAULT_FONT_MARKER
    suspend fun setSelectedFont(font: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_FONT] = font
        }
    }

    // 读取字体列表时，在最前面加入默认字体标识
    val fontsList: Flow<List<String>> = dataStore.data.map { prefs ->
        val storedFonts = prefs[FONT_LIST]
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?: listOf()
        listOf(DEFAULT_FONT_MARKER) + storedFonts
    }.distinctUntilChanged()

    // 当前选中的字体 Flow，默认值为 DEFAULT_FONT_MARKER
    val selectedFont: Flow<String> = dataStore.data.map { prefs ->
        if (prefs[SELECTED_FONT]?.let { prefs[FONT_LIST]?.contains(it) } == true) {
            prefs[SELECTED_FONT] ?: DEFAULT_FONT_MARKER
        }else {
            DEFAULT_FONT_MARKER
        }
    }.distinctUntilChanged()

    val isEasterEggEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[EASTER_EGG] ?: false
    }.distinctUntilChanged()

    suspend fun updateEasterEggState(state: Boolean) {
        dataStore.edit { prefs ->
            prefs[EASTER_EGG] = state
        }
    }

    val isTooDeep: Flow<Boolean> = dataStore.data.map { prefs ->
        val deepState = prefs[IS_TOO_DEEP] ?: false
        val easterEggState = prefs[EASTER_EGG] ?: false
        deepState and easterEggState
    }.distinctUntilChanged()

    suspend fun updateTooDeepState(state: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_TOO_DEEP] = state
        }
    }

    val mosaicMode: Flow<Int> = dataStore.data.map { prefs ->
        prefs[MOSAIC_MODE] ?: MOSAIC_MODE_EMOJI
    }.distinctUntilChanged()

    suspend fun setMosaicMode(mode: Int) {
        if (mode == MOSAIC_MODE_EMOJI || mode == MOSAIC_MODE_BLUR) {
            // 仅允许设置为已定义的模式
            dataStore.edit { prefs ->
                prefs[MOSAIC_MODE] = mode
            }
        } else {
            throw IllegalArgumentException("Invalid mosaic mode: $mode")
        }
    }

    val mosaicType: Flow<Int> = dataStore.data.map { prefs ->
        prefs[MOSAIC_TYPE] ?: MOSAIC_TYPE_GAUSSIAN
    }.distinctUntilChanged()

    suspend fun setMosaicType(type: Int) {
        if (type == MOSAIC_TYPE_GAUSSIAN || type == MOSAIC_TYPE_PIXELATED || type == MOSAIC_TYPE_HALFTONE) {
            // 仅允许设置为已定义的类型
            dataStore.edit { prefs ->
                prefs[MOSAIC_TYPE] = type
            }
        } else {
            throw IllegalArgumentException("Invalid mosaic type: $type")
        }
    }

    val mosaicTarget: Flow<Int> = dataStore.data.map { prefs ->
        prefs[MOSAIC_TARGET] ?: MOSAIC_TARGET_FACE
    }.distinctUntilChanged()

    suspend fun setMosaicTarget(target: Int) {
        if (target == MOSAIC_TARGET_FACE || target == MOSAIC_TARGET_EYES) {
            // 仅允许设置为已定义的目标
            dataStore.edit { prefs ->
                prefs[MOSAIC_TARGET] = target
            }
        } else {
            throw IllegalArgumentException("Invalid mosaic target: $target")
        }
    }
}