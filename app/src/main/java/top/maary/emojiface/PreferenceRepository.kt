package top.maary.emojiface

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
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
}