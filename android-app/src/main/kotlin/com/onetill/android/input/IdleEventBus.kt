package com.onetill.android.input

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IdleEventBus {
    private val _lastTouchTime = MutableStateFlow(System.currentTimeMillis())
    val lastTouchTime: StateFlow<Long> = _lastTouchTime.asStateFlow()

    fun onTouch() {
        _lastTouchTime.value = System.currentTimeMillis()
    }
}
