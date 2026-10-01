package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * HifzReciterLock:
 *
 * Persists the Hifz-mode reciter lock (the single reciter slug the Hifz
 * session is pinned to, or unlocked when null).
 *
 * Stored via multiplatform [Settings] under one key ([KEY_LOCK_SLUG]);
 * empty/missing means unlocked. Follows the [HifzMasteryStore] pattern:
 * lazy [Settings], in-memory [StateFlow], synchronous persist on mutation,
 * silent try/catch on every storage access. Pure common code.
 *
 * ## Sync status: owner-agnostic, NOT synced
 * The key is a single device-global entry with no per-owner namespacing, so
 * a sign-in / account switch keeps whatever lock was set while signed out.
 * Per-owner namespacing (`<userId>::` prefix + first-owner adoption, as in
 * [HifzMasteryStore.setOwner]) can follow later if the lock needs to roam
 * with the account; until then do not assume a lock set on one device or
 * account is visible anywhere else.
 */
object HifzReciterLock {
    private const val KEY_LOCK_SLUG = "hifz_reciter_lock_slug"

    private val settings: Settings by lazy { Settings() }

    private val _lockedSlug = MutableStateFlow<String?>(null)
    val lockedSlug: StateFlow<String?> = _lockedSlug.asStateFlow()

    init {
        load()
    }

    /**
     * Pins Hifz playback to [slug]. A blank slug is treated as [unlock] so
     * an empty string is never stored as a locked value.
     */
    fun lock(slug: String) {
        val normalized = slug.trim()
        if (normalized.isEmpty()) {
            unlock()
            return
        }
        _lockedSlug.value = normalized
        try {
            settings.putString(KEY_LOCK_SLUG, normalized)
        } catch (_: Exception) {
        }
    }

    /** Clears the reciter lock (unlocked). */
    fun unlock() {
        _lockedSlug.value = null
        try {
            settings.putString(KEY_LOCK_SLUG, "")
        } catch (_: Exception) {
        }
    }

    private fun load() {
        val saved = try {
            settings.getString(KEY_LOCK_SLUG, "")
        } catch (_: Exception) {
            ""
        }.trim()
        _lockedSlug.value = saved.ifEmpty { null }
    }
}
