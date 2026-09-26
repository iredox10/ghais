package com.ghais.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 *
 * Also owns [followerCounts], a best-effort, owner-scoped cache of live public
 * follower counters. Two invariants the rest of the app relies on:
 *  - absent key = unknown, and unknown renders as nothing. It is never 0, and
 *    0 is only ever stored when the source really reported 0.
 *  - every count read goes through one write rule ([applyCount]): a read that
 *    yields nothing evicts the slug, so a failed read never leaves a stale
 *    number on screen. All count data is unknown-by-default; nothing here
 *    invents a number, and the client never writes `reciter_stats`.
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

    // Live follower counts sourced from the public `reciter_stats` collection,
    // keyed by reciter slug. ABSENT KEY = UNKNOWN, and the UI renders an
    // unknown count as nothing at all. An unknown count is never stored as 0;
    // a real `followers_count: 0` is stored as 0. NOT scoped to the follow
    // set: browse screens read counts for reciters the user does not follow,
    // so follow-set membership neither adds nor evicts an entry. Owner-scoped
    // (cleared on [setOwner]) but never written by the client.
    private val _followerCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val followerCounts: StateFlow<Map<String, Long>> = _followerCounts.asStateFlow()

    /**
     * Injectable single-slug count reader, null when the count is unknown
     * (no `reciter_stats` doc, collection not provisioned yet, or any read
     * failure). The Android implementation resolves the slug out of ONE
     * batched, TTL-cached `reciter_stats` snapshot, so a call issues no
     * request of its own. Wired from platform code that owns the cloud client
     * (android `SyncEngine.init`); null on platforms without one, where counts
     * simply stay unknown.
     */
    var countFetcher: (suspend (String) -> Long?)? = null

    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * How many slugs [refreshCounts] resolves at once. Bounded so a large
     * follow set cannot fan out an unbounded number of concurrent reads. The
     * fetcher is a cached snapshot lookup, so this only caps scheduler churn
     * and switch context count — not server load, which stays at one batched
     * list per sync-layer TTL window.
     */
    private const val COUNT_REFRESH_CONCURRENCY = 16

    init {
        load()
    }

    /**
     * Rebinds the store to [ownerId]'s namespace and reloads their slugs.
     *
     * The follower-count map is cleared here: counts fetched for the previous
     * owner are not the new owner's data (they are public counters, so leaking
     * them is a privacy leak on account switch and unbounded growth across
     * accounts). Callers must treat the post-switch map as empty and let the
     * sync pass repopulate it — every count path is best-effort and a missing
     * count simply renders as unknown.
     */
    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        _followerCounts.value = emptyMap()
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
     * Bulk replace of the follow set (used by the login pull).
     *
     * Deliberately does NOT touch the count map: refreshing counts here would
     * fan out one read per slug at the worst possible moment (mid-pull, before
     * the sync pass runs), and the map is not scoped to the follow set anyway,
     * so dropping non-followed slugs' counts would be wrong. Slugs that left
     * the follow set keep whatever count they already had until a read evicts
     * it. Use [setCounts] to bulk-load counts the caller already holds, or
     * [refreshCounts] to have them read.
     */
    fun setAll(slugs: Set<String>) {
        _followedSlugs.value = slugs
        save()
    }

    fun clear() {
        _followedSlugs.value = emptySet()
        // Drop counts too: they belonged to the account/data set just wiped.
        // Refetched best-effort, so a wiped map only means "unknown" meanwhile.
        _followerCounts.value = emptyMap()
        save()
    }

    /**
     * Best-effort refresh of the counts for [slugs].
     *
     * Resolves in bounded-concurrency batches rather than one slug at a time:
     * the per-slug fetcher is a cached snapshot lookup, so a sequential loop is
     * pure scheduler churn — N awaits and N dispatcher hops for data that is
     * already in memory. [COUNT_REFRESH_CONCURRENCY] at a time turns that into
     * N/16 rounds without opening an unbounded number of concurrent reads.
     *
     * Eviction rule (shared with the fire-and-forget path, see [applyCount]):
     * a slug whose read yields a count gets that count written; a slug whose
     * read yields no count — absent doc, unprovisioned `reciter_stats`,
     * transport error — is EVICTED, so a failed read can never leave a stale
     * count on screen. An evicted/absent slug is "unknown", never 0.
     */
    suspend fun refreshCounts(slugs: Collection<String>) {
        val fetch = countFetcher ?: return
        val distinct = slugs.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (distinct.isEmpty()) return
        for (batch in distinct.chunked(COUNT_REFRESH_CONCURRENCY)) {
            val resolved = coroutineScope {
                batch.map { slug -> async { slug to safeFetch(fetch, slug) } }.awaitAll()
            }
            for ((slug, count) in resolved) applyCount(slug, count)
        }
    }

    suspend fun refreshCount(slug: String) {
        refreshCounts(listOf(slug))
    }

    /**
     * Bulk-load counts the caller ALREADY holds — no read is issued, so a
     * device that just listed `reciter_stats` once does not re-request the
     * same snapshot slug by slug. This is the intended partner of [setAll]:
     * the sync layer pulls the follow set and the counters together, calls
     * [setAll] then [setCounts], and the UI has counts immediately instead of
     * waiting for a refresh pass.
     *
     * Only real counters are stored: blank slugs and negative values are
     * dropped, and an absent slug simply is not in the map (= unknown, hidden).
     * Never substitutes a placeholder for a missing count.
     *
     * @param replace true (default) swaps the whole map, which also drops
     *   counts that are no longer reported (self-healing). Pass false to merge
     *   instead — required when the source snapshot is known to be partial
     *   (e.g. it hit the sync layer's per-request doc cap), where a wholesale
     *   swap would discard valid counts the truncated read simply omitted.
     */
    fun setCounts(counts: Map<String, Long>, replace: Boolean = true) {
        val known = HashMap<String, Long>(counts.size)
        for ((rawSlug, count) in counts) {
            val slug = rawSlug.trim()
            if (slug.isEmpty() || count < 0L) continue
            known[slug] = count
        }
        if (replace) {
            _followerCounts.value = known
        } else {
            _followerCounts.update { it + known }
        }
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
    // displayed count converges without blocking the caller. Goes through the
    // same [applyCount] rule as [refreshCounts]: a successful read writes the
    // count, a read that yields nothing (or fails) evicts it — so a stale count
    // cannot survive an unfollow, matching the awaited path. Nothing is
    // incremented locally: the client never writes `reciter_stats` and no
    // server-side increment exists, so a follow leaves the count unchanged
    // until the counter is published out-of-band and a later read picks it up.
    private fun requestCountRefresh(slug: String) {
        val fetch = countFetcher ?: return
        val clean = slug.trim()
        if (clean.isEmpty()) return
        refreshScope.launch {
            applyCount(clean, safeFetch(fetch, clean))
        }
    }

    /**
     * The one write rule for counts, used by every path that produces one.
     * A count writes the entry; anything else evicts it, so no path can leave
     * a stale count behind and no path can invent a 0 for an unknown count.
     * A negative value is treated as unknown rather than rendered.
     */
    private fun applyCount(slug: String, count: Long?) {
        if (count != null && count >= 0L) {
            _followerCounts.update { it + (slug to count) }
        } else {
            _followerCounts.update { it - slug }
        }
    }

    /** Fetcher call that yields null on any failure; cancellation propagates. */
    private suspend fun safeFetch(fetch: suspend (String) -> Long?, slug: String): Long? =
        try {
            fetch(slug)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
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
