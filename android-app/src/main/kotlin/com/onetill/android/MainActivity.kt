package com.onetill.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.onetill.android.ui.navigation.OneTillNavGraph
import com.onetill.android.ui.theme.OneTillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OneTillTheme {
                OneTillNavGraph()
            }
        }
    }
}
