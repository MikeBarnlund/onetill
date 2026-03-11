package com.onetill.android.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onetill.shared.auth.StaffAuthManager
import com.onetill.shared.data.model.StaffUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class LockViewModel(
    private val staffAuthManager: StaffAuthManager,
) : ViewModel() {

    private val staffUsers: StateFlow<List<StaffUser>> =
        staffAuthManager.observeStaffUsers()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hasUsers: StateFlow<Boolean> =
        staffAuthManager.hasUsers
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    val shouldShowLockScreen: StateFlow<Boolean> =
        kotlinx.coroutines.flow.combine(isLocked, hasUsers) { locked, users ->
            locked && users
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun verifyPin(pin: String): Boolean {
        val match = staffAuthManager.verifyPin(pin, staffUsers.value)
        if (match) {
            _isLocked.value = false
        }
        return match
    }

    fun lock() {
        _isLocked.value = true
    }
}
