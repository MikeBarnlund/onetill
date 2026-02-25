package com.onetill.shared.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-provided network state monitor.
 * Implementations injected via Koin from android-app/ (ConnectivityManager) or iosMain/ later.
 */
interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}
