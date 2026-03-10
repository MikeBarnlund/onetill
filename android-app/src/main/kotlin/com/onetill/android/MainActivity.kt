package com.onetill.android

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.onetill.android.audio.ScanBeepPlayer
import com.onetill.android.input.IdleEventBus
import com.onetill.android.input.VolumeKeyEvent
import com.onetill.android.input.VolumeKeyEventBus
import com.onetill.android.ui.navigation.OneTillNavGraph
import com.onetill.android.ui.theme.OneTillTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ScanBeepPlayer.init(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            OneTillTheme {
                OneTillNavGraph()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = 2 // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_GESTURE
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        IdleEventBus.onTouch()
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        IdleEventBus.onTouch()
        if (VolumeKeyEventBus.isActive && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        VolumeKeyEventBus.emit(VolumeKeyEvent.Pressed)
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    VolumeKeyEventBus.emit(VolumeKeyEvent.Released)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
