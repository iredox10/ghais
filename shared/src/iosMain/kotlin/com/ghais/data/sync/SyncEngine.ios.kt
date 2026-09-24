package com.ghais.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub: auth SDK is JVM-only so there is no live sync on iOS yet.
 * All data stays local; the UI shows the disabled state.
 */
actual object SyncEngine {

    private val _status = MutableStateFlow(SyncStatus.DISABLED)
    actual val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    actual val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    actual val lastError: StateFlow<String?> = _lastError.asStateFlow()

    actual suspend fun syncNow() {
        // No-op until a Native HTTP sync path lands.
    }

    actual suspend fun refreshEditorial() {
        // No-op until a Native HTTP sync path lands (shelf stays empty on iOS).
    }
}
