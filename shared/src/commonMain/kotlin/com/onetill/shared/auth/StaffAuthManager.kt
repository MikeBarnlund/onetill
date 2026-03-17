package com.onetill.shared.auth

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.StaffUser
import com.onetill.shared.util.sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class StaffAuthManager(
    private val localDataSource: LocalDataSource,
) {
    val hasUsers: Flow<Boolean> =
        localDataSource.observeStaffUsers().map { it.isNotEmpty() }

    private val _authenticatedUser = MutableStateFlow<StaffUser?>(null)
    val authenticatedUser: StateFlow<StaffUser?> = _authenticatedUser.asStateFlow()

    /** Display name of the currently authenticated staff member, or null if no PIN lock. */
    val currentStaffName: String?
        get() = _authenticatedUser.value?.let { "${it.firstName} ${it.lastName}".trim() }
            ?.ifEmpty { null }

    fun observeStaffUsers(): Flow<List<StaffUser>> =
        localDataSource.observeStaffUsers()

    /**
     * Verify a PIN against a cached list of staff users.
     * On match, stores the authenticated user for audit trail purposes.
     * Returns true if any staff user's pinSha256 matches the SHA-256 of [pin].
     */
    fun verifyPin(pin: String, staffUsers: List<StaffUser>): Boolean {
        val hash = sha256(pin)
        val matched = staffUsers.firstOrNull { it.pinSha256 == hash }
        if (matched != null) {
            _authenticatedUser.value = matched
        }
        return matched != null
    }

    fun clearAuthenticatedUser() {
        _authenticatedUser.value = null
    }
}
