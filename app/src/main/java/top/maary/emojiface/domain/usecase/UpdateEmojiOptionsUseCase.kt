package top.maary.emojiface.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.maary.emojiface.datastore.PreferenceRepository
import top.maary.emojiface.util.splitEmoji // 确认 splitEmoji 的路径
import javax.inject.Inject

class UpdateEmojiOptionsUseCase @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) {

    /**
     * 更新存储在 Preference 中的预定义 Emoji 列表。
     *
     * @param emojisString 一个包含用户输入 Emoji 的字符串，Emoji 之间无需分隔符。如果为空，将重置为默认列表。
     * @return Result<Unit> 指示操作是否成功。
     */
    suspend operator fun invoke(emojisString: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val emojiList = if (emojisString.isEmpty()) {
                // 如果输入为空，使用仓库中定义的默认列表
                PreferenceRepository.DEFAULT_EMOJI_LIST
            } else {
                // 使用工具函数安全地分割 Emoji 字符串
                splitEmoji(emojisString)
            }

            // 更新 DataStore
            preferenceRepository.updateEmojiOptions(emojiList)
        }.onFailure {
             android.util.Log.e("UpdateEmojiOptionsUseCase", "Failed to update emoji options", it)
        }
    }
}
