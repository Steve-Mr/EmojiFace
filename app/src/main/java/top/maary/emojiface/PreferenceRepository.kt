package top.maary.emojiface

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import top.maary.emojiface.Constants.DEFAULT_FONT_MARKER
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
    }

    // 从 DataStore 中读取 emoji 列表（以逗号分隔存储）
    val emojiOptionsFlow: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[EMOJI_LIST]?.let { jsonString ->
            // 反序列化，如果失败则返回 null
            runCatching { Json.decodeFromString<EmojiList>(jsonString) }.getOrNull()?.emojis
        } ?: DEFAULT_EMOJI_LIST
    }

    // 更新 DataStore 中的 emoji 列表
    suspend fun updateEmojiOptions(newOptions: List<String>) {
        val emojiList = EmojiList(newOptions)
        val jsonString = Json.encodeToString(emojiList)
        dataStore.edit { preferences ->
            preferences[EMOJI_LIST] = jsonString
        }
    }

    val isIconHide: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_ICON_HIDE] ?: false
    }

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
    }

    // 当前选中的字体 Flow，默认值为 DEFAULT_FONT_MARKER
    val selectedFont: Flow<String> = dataStore.data.map { prefs ->
        if (prefs[SELECTED_FONT]?.let { prefs[FONT_LIST]?.contains(it) } == true) {
            prefs[SELECTED_FONT] ?: DEFAULT_FONT_MARKER
        }else {
            DEFAULT_FONT_MARKER
        }
    }
}