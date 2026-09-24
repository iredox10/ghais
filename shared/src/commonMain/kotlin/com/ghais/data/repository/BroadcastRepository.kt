package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Public announcement from the `broadcasts` collection
 * (`broadcasts{title,body,audience,urgency}`, PUBLIC_READ, DB `ghais`).
 * Server orders newest-first (`$createdAt` desc, limit 50); the client keeps
 * that order as-is. `createdAt` is the raw Appwrite ISO timestamp (no local
 * parsing — display only).
 */
data class Broadcast(
    val id: String,
    val title: String,
    val body: String,
    val audience: String = "all",
    val urgency: String = "Normal",
    val createdAt: String = "",
)

/**
 * In-memory broadcasts inbox cache + local read state.
 *
 * - Cloud pull is injected via [cloudFetcher] (wired once by
 *   `SyncEngine.init` on Android, same pattern as
 *   `FollowStore.countFetcher`; null on iOS so [refresh] is a no-op there).
 * - [refresh] pulls newest-first into [broadcasts]; never throws.
 * - Read state is a local-only set of seen doc ids persisted as one
 *   comma-joined string under `ghais_broadcasts_seen` (global, not
 *   owner-namespaced — broadcasts are public, not per-account).
 */
object BroadcastRepository {
    private const val KEY_SEEN = "ghais_broadcasts_seen"

    private val settings: Settings by lazy { Settings() }

    private val _broadcasts = MutableStateFlow<List<Broadcast>>(emptyList())
    val broadcasts: StateFlow<List<Broadcast>> = _broadcasts.asStateFlow()

    private val _seenIds = MutableStateFlow(loadSeen())
    val seenIds: StateFlow<Set<String>> = _seenIds.asStateFlow()

    var cloudFetcher: (suspend () -> List<Broadcast>)? = null

    suspend fun refresh(): Boolean {
        return try {
            val list = cloudFetcher?.invoke() ?: return false
            _broadcasts.value = list
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isSeen(id: String): Boolean = id in _seenIds.value

    fun markSeen(id: String) {
        if (id.isBlank() || id in _seenIds.value) return
        val next = _seenIds.value + id
        _seenIds.value = next
        try {
            settings.putString(KEY_SEEN, next.joinToString(","))
        } catch (_: Exception) {
        }
    }

    private fun loadSeen(): Set<String> {
        return try {
            settings.getString(KEY_SEEN, "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
}
