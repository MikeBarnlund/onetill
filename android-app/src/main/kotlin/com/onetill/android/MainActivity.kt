package com.onetill.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onetill.android.ui.navigation.OneTillNavGraph
import com.onetill.android.ui.theme.OneTillTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system UI to simulate S700 (no system chrome)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = 2 // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_GESTURE
        }

        setContent {
            OneTillTheme {
                OneTillNavGraph()
            }
        }
    }
}
