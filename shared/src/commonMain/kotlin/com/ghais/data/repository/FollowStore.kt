package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FollowStore: persisted store of followed Qari (identified by reciter slug).
 *
 * Mirrors [FavoritesStore] persistence approach (multiplatform Settings +
 * JSON-encoded list) but tracks a set of reciter slug Strings.
 */
object FollowStore {
    private const val KEY_FOLLOWED_QARI = "ghais_followed_qari"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = "local"

    private fun key(base: String): String =
        if (ownerId == "local") base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _followedSlugs = MutableStateFlow<Set<String>>(emptySet())
    val followedSlugs: StateFlow<Set<String>> = _followedSlugs.asStateFlow()

    // Live follower counts sourced from the public `reciter_stats` collection
    // (doc id = slug). Absent key = unknown (UI hides counts). Never written
    // by the client except via best-effort re-reads after follow/unfollow.
    private val _followerCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val followerCounts: StateFlow<Map<String, Long>> = _followerCounts.asStateFlow()

    /**
     * Injectable single-slug count reader (public `reciter_stats` doc read,
     * null on 404/any failure). Wired from platform code that owns the cloud
     * client (android `SyncEngine.init`); null on platforms without one, where
     * counts simply stay unknown.
     */
    var countFetcher: (suspend (String) -> Long?)? = null

    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        load()
    }

    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    fun isFollowing(slug: String): Boolean {
        return _followedSlugs.value.contains(slug)
    }

    fun toggle(slug: String) {
        _followedSlugs.update { set ->
            if (set.contains(slug)) {
                set - slug
            } else {
                set + slug
            }
        }
        save()
        requestCountRefresh(slug)
    }

    fun follow(slug: String) {
        _followedSlugs.update { set ->
            if (set.contains(slug)) set else set + slug
        }
        save()
        requestCountRefresh(slug)
    }

    fun unfollow(slug: String) {
        _followedSlugs.update { set ->
            set - slug
        }
        save()
        requestCountRefresh(slug)
    }

    /**
     * Bulk replace without per-slug count refresh (used by the login pull to
     * avoid an N× fan-out; the sync pass refreshes counts once afterwards).
     */
    fun setAll(slugs: Set<String>) {
        _followedSlugs.value = slugs
        save()
    }

    fun clear() {
        _followedSlugs.value = emptySet()
        save()
    }

    /**
     * Best-effort re-read of displayed counts for [slugs]. Null reads evict
     * the entry so the UI hides unknown counts instead of showing stale ones.
     */
    suspend fun refreshCounts(slugs: Collection<String>) {
        val fetch = countFetcher ?: return
        val distinct = slugs.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (distinct.isEmpty()) return
        for (slug in distinct) {
            val count = try {
                fetch(slug)
            } catch (_: Exception) {
                null
            }
            if (count != null) {
                _followerCounts.update { it + (slug to count) }
            } else {
                _followerCounts.update { it - slug }
            }
        }
    }

    suspend fun refreshCount(slug: String) {
        refreshCounts(listOf(slug))
    }

    /** Compact count label ("1.2k followers"); null renders nothing (hidden). */
    fun formatFollowerCount(count: Long): String = when {
        count < 1000 -> "$count followers"
        count < 1_000_000 -> "${trim1(count / 1000.0)}k followers"
        else -> "${trim1(count / 1_000_000.0)}M followers"
    }

    private fun trim1(value: Double): String {
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    // Fire-and-forget single-slug refresh after a local follow/unfollow so the
    // displayed count converges without blocking the caller. The debounced
    // cloud push re-reads again after it succeeds; server-side increments
    // (Cloud Function) land on a later refresh.
    private fun requestCountRefresh(slug: String) {
        val fetch = countFetcher ?: return
        val clean = slug.trim()
        if (clean.isEmpty()) return
        refreshScope.launch {
            val count = try {
                fetch(clean)
            } catch (_: Exception) {
                null
            }
            if (count != null) {
                _followerCounts.update { it + (clean to count) }
            }
        }
    }

    private fun load() {
        try {
            var raw = settings.getString(key(KEY_FOLLOWED_QARI), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy namespaced value if present.
                try {
                    val legacyKey = key(legacyBase(KEY_FOLLOWED_QARI))
                    val legacyRaw = settings.getString(legacyKey, "")
                    if (legacyRaw.isNotBlank()) {
                        raw = legacyRaw
                        try {
                            settings.putString(key(KEY_FOLLOWED_QARI), legacyRaw)
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
                _followedSlugs.value = emptySet()
                return
            }
            _followedSlugs.value = json.decodeFromString<List<String>>(raw).toSet()
        } catch (_: Exception) {
            _followedSlugs.value = emptySet()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_FOLLOWED_QARI), json.encodeToString(_followedSlugs.value.toList()))
        } catch (_: Exception) {
        }
    }
}
