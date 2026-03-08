package com.onetill.android.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class VolumeKeyEvent {
    data object Pressed : VolumeKeyEvent()
    data object Released : VolumeKeyEvent()
}

object VolumeKeyEventBus {
    private val _events = MutableSharedFlow<VolumeKeyEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    var isActive: Boolean = false

    fun emit(event: VolumeKeyEvent) {
        _events.tryEmit(event)
    }
}
