package com.onetill.android.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.StaffUser
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LockViewModel(
    private val localDataSource: LocalDataSource,
) : ViewModel() {

    init {
        Napier.i("LockViewModel: created")
    }

    private val staffUsers: StateFlow<List<StaffUser>> =
        localDataSource.observeStaffUsers()
            .onEach { Napier.i("LockViewModel: staffUsers emitted ${it.size} users") }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasUsers: StateFlow<Boolean> =
        localDataSource.observeStaffUsers()
            .map { it.isNotEmpty() }
            .onEach { Napier.i("LockViewModel: hasUsers = $it") }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /**
     * Whether the lock screen should actually be shown.
     * Only lock if there are staff users configured.
     */
    val shouldShowLockScreen: StateFlow<Boolean> =
        kotlinx.coroutines.flow.combine(isLocked, hasUsers) { locked, users ->
            Napier.i("LockViewModel: shouldShowLock = locked=$locked, hasUsers=$users → ${locked && users}")
            locked && users
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun verifyPin(pin: String): Boolean {
        val sha256 = sha256(pin)
        val match = staffUsers.value.any { it.pinSha256 == sha256 }
        if (match) {
            _isLocked.value = false
        }
        return match
    }

    fun lock() {
        _isLocked.value = true
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
