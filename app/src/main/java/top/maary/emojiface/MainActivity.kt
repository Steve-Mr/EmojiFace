package top.maary.emojiface

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.util.Consumer
import dagger.hilt.android.AndroidEntryPoint
import top.maary.emojiface.ui.edit.EditScreen
import top.maary.emojiface.ui.theme.EmojiFaceTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _newIntents = MutableSharedFlow<Intent>(replay = 1)
    val newIntents = _newIntents.asSharedFlow()

    private val intentListener = Consumer<Intent> { intent ->
        lifecycleScope.launch {
            _newIntents.emit(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        addOnNewIntentListener(intentListener)
        setContent {
            EmojiFaceTheme {
                EditScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOnNewIntentListener(intentListener)
    }
}
