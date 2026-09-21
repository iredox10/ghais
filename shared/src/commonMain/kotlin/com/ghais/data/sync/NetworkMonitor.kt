package com.ghais.data.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform connectivity probe for gating cloud sync.
 *
 * `true` = device currently has a usable network path; `false` = offline.
 * [SyncTriggers] only runs [SyncEngine.syncNow] while online with an active
 * session, and runs one catch-up pass on each offline→online transition.
 *
 * Fail-open: all implementations default to `true` until the platform has
 * determined otherwise, so sync is never permanently blocked by a missing
 * probe.
 */
expect object NetworkMonitor {
    /** Observable connectivity; `true` = online. */
    val isOnline: StateFlow<Boolean>
}
