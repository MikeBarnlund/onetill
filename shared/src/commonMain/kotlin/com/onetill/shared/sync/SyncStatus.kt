package com.onetill.shared.sync

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

data class SyncProgress(
    val currentPage: Int,
    val totalProducts: Int,
    val isComplete: Boolean,
)
