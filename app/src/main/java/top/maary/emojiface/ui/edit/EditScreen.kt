package top.maary.emojiface.ui.edit

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass

@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel(),
               windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass) {
    EditScreenContentInternal(emojiViewModel, windowSizeClass)

}



