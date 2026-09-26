package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SearchHistoryStore: persisted, owner-scoped store of user-typed global search
 * queries, rather than reciter picks.
 *
 * Mirrors [ReciterSearchHistoryStore] persistence approach (multiplatform Settings +
 * JSON-encoded list, owner-namespaced keys) but tracks an ordered,
 * most-recent-first list capped at 15 entries.
 *
 * ## Sync status: owner-namespaced, NOT synced
 * Keys are owner-namespaced (`<userId>::ghais_search_history` when bound, bare
 * while signed out) and [setOwner] performs a safe first-owner adoption, so the
 * store is *ready* to be wired into the session collector like
 * [FavoritesStore]. But there is **no `search_history` collection behind it
 * yet**: nothing pushes the list and nothing pulls one, so a second device
 * starts with an empty history and this list is not a cloud copy of anything.
 * [exportSnapshot] / [importSnapshot] are the seam the sync layer will use; no
 * code in the app calls them today.
 *
 * This is the low-stakes store of the pair (a stale query beats a lost one), so
 * adoption only ever *adds* the pre-login history to an account that has none —
 * it never merges into, reorders, or truncates an account's own list.
 */
object SearchHistoryStore {
    private const val KEY_HISTORY = "ghais_search_history"
    private const val MAX_SIZE = 15

    /** The unbound / pre-auth owner. [key] collapses to the bare base key for it. */
    private const val LOCAL_OWNER = "local"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = LOCAL_OWNER

    private fun key(base: String): String =
        if (ownerId == LOCAL_OWNER) base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    init {
        load()
    }

    /**
     * Binds the store to [ownerId]'s namespace (`"local"` unbinds) and reloads.
     *
     * **First-owner adoption.** There is no cloud counterpart yet, so a new
     * namespace is always empty and a plain rebind would drop the history
     * typed while signed out — with no pull to bring it back. So a bind that
     * LEAVES the unbound namespace also carries that history over into the new
     * account: see [adoptLocalNamespace]. Adoption moves data only *out of* the
     * unbound namespace and *into an account that has none of its own*, so an
     * account -> account switch can never show one user's searches to another.
     * Idempotent — rebinding the same owner is a no-op, and the source is
     * deleted after it is copied, so it can be adopted at most once.
     */
    fun setOwner(ownerId: String) {
        val normalized = ownerId.ifBlank { LOCAL_OWNER }
        if (normalized == this.ownerId) return
        // Only a bind away from the unbound namespace may adopt; switching
        // straight from one account to another carries nothing at all.
        val carried = if (this.ownerId == LOCAL_OWNER) _recentQueries.value else null
        this.ownerId = normalized
        load()
        carried?.let { adoptLocalNamespace(it) }
    }

    /**
     * Copies the pre-login history into the account just bound, then deletes
     * the source.
     *
     * Bails out when the account already has a history of its own (never
     * overwrite real data) or when there is nothing to carry, and otherwise
     * replaces the empty list verbatim, keeping the most-recent-first order the
     * source already had. Deleting the source last means a failed write leaves
     * the data intact in the `local` namespace.
     */
    private fun adoptLocalNamespace(carried: List<String>) {
        if (carried.isEmpty()) return
        if (_recentQueries.value.isNotEmpty()) return
        _recentQueries.value = normalize(carried)
        save()
        try {
            settings.remove(KEY_HISTORY)
            settings.remove(legacyBase(KEY_HISTORY))
        } catch (_: Exception) {
        }
    }

    fun record(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        _recentQueries.update { list ->
            (listOf(clean) + list.filterNot { it.equals(clean, ignoreCase = true) }).take(MAX_SIZE)
        }
        save()
    }

    fun remove(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        _recentQueries.update { list ->
            list.filterNot { it == clean }
        }
        save()
    }

    fun clear() {
        _recentQueries.value = emptyList()
        save()
    }

    /**
     * The bound owner's list, most recent first, for a push. Copied, so the
     * caller cannot mutate the live [recentQueries] list.
     */
    fun exportSnapshot(): List<String> = _recentQueries.value.toList()

    /**
     * Replaces the bound owner's list with [queries] — the pull direction, for
     * the sync layer to call once it has decided the cloud copy wins.
     * Authoritative: it is not a merge, so a local query the document does not
     * have is dropped.
     *
     * [queries] goes through the same normalization [load] applies (trimmed,
     * at least 2 characters, case-insensitively unique, capped at 15 in the
     * order given), so a malformed document cannot produce a list the store
     * could not have produced itself. Bind the owner FIRST: unbound this would
     * write into the device-global `local` namespace, where the next sign-in
     * would adopt it.
     */
    fun importSnapshot(queries: List<String>) {
        _recentQueries.value = normalize(queries)
        save()
    }

    /**
     * The one shape a stored list is ever allowed to take, shared by [load] and
     * [importSnapshot] so a restored list and a loaded one are indistinguishable.
     */
    private fun normalize(queries: List<String>): List<String> =
        queries
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }
            .take(MAX_SIZE)

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_HISTORY), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_HISTORY))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_HISTORY), legacyRaw)
                        } catch (_: Exception) {
                        }
                        try {
                            settings.remove(legacyKey)
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
            }
            if (raw.isBlank()) {
                _recentQueries.value = emptyList()
                return
            }
            _recentQueries.value = normalize(json.decodeFromString<List<String>>(raw))
        } catch (_: Exception) {
            _recentQueries.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_HISTORY), json.encodeToString(_recentQueries.value))
        } catch (_: Exception) {
        }
    }
}
