package top.maary.emojiface.ui.edit.state

import android.net.Uri

sealed class ShareEvent {
    data class ShareImage(val uri: Uri) : ShareEvent()
    data class Error(val message: String, val status: Int) : ShareEvent()
    data class Success(val status: Int) : ShareEvent()
}