package com.onetill.shared.auth

import com.onetill.shared.data.local.LocalDataSource
import com.onetill.shared.data.model.StaffUser
import com.onetill.shared.util.sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StaffAuthManager(
    private val localDataSource: LocalDataSource,
) {
    val hasUsers: Flow<Boolean> =
        localDataSource.observeStaffUsers().map { it.isNotEmpty() }

    fun observeStaffUsers(): Flow<List<StaffUser>> =
        localDataSource.observeStaffUsers()

    /**
     * Verify a PIN against a cached list of staff users.
     * Returns true if any staff user's pinSha256 matches the SHA-256 of [pin].
     */
    fun verifyPin(pin: String, staffUsers: List<StaffUser>): Boolean {
        val hash = sha256(pin)
        return staffUsers.any { it.pinSha256 == hash }
    }
}
