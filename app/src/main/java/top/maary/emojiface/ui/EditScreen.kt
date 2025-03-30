package top.maary.emojiface.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import top.maary.emojiface.EmojiViewModel

@Composable
fun EditScreen(emojiViewModel: EmojiViewModel = viewModel(),
               windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass) {
    when{
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            EditScreenLarge(emojiViewModel = emojiViewModel, isMedium = false)
        }
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            EditScreenLarge(emojiViewModel = emojiViewModel, isMedium = true)
        }
        else -> {
            EditScreenCompact(emojiViewModel)
        }
    }

}