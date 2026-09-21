package com.ghais.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub: no Native reachability path is wired yet, so this stays
 * fail-open (`true` = always online) until an NWPathMonitor-based
 * implementation lands. Sync gating treats the device as online.
 */
actual object NetworkMonitor {

    private val _isOnline = MutableStateFlow(true)
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}
