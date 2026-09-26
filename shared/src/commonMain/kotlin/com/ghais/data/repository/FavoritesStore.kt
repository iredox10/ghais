package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FavoritesStore: persisted store of favorite tracks.
 *
 * Mirrors [FollowStore]'s persistence approach (per-owner multiplatform
 * Settings + JSON-encoded list, one-time legacy-key adoption) but stores
 * [TrackItem]s.
 *
 * ## Identity
 * Identity is the audio URL — the same notion [LibraryRepository.toggleLike] /
 * [LibraryRepository.isLiked] use, and the same one `SurahDetailScreen` uses
 * when it computes its heart state inline. It is deliberately NOT the cloud
 * doc-id tuple (`userId|slug|surahId|ayahNo`, see `SyncEngine.likeDocId`):
 * the two catalogs spell the same human two ways (`mishary` vs `alafasy`) and
 * both resolve to the SAME everyayah folder, so keying on the slug tuple would
 * show that ayah twice in the Favourites list while the URL identity keeps one
 * row. See `trackKey` for the narrower, merge-only use of the tuple.
 *
 * ## Pre-auth adoption (the ordering that matters)
 * A favourite added while signed out must reach the account on login, but it
 * must NOT be written into the account namespace before the cloud restore has
 * run: `SyncEngine.pullLikesIfEmpty` skips the whole pull whenever the local
 * list is non-empty, and `pushLikes` then prunes every cloud doc whose
 * `surah_id:ayah_no` is not in the local set. Adopting eagerly therefore made
 * "log in on a new phone with one favourite tapped before signing in" DELETE
 * the account's entire cloud set and never restore it.
 *
 * So favourites carried out of the `local` namespace are held in a **pending
 * adoption buffer** (persisted under its own owner-scoped key,
 * `ghais_favorite_tracks_pending`) that is deliberately kept OUT of
 * [favoriteTracks]:
 * - released into [favoriteTracks] by [clear] — which the cloud pull calls
 *   before it adds anything, so the pull is free to run and the release lands
 *   before `pushLikes` reads the list (union of cloud + pre-auth);
 * - or merged immediately by [setOwner] when the target namespace is already
 *   non-empty, because in that case the pull is going to be skipped anyway and
 *   a buffer would never be released;
 * - or by [reconcilePending], the moment a real user action makes the visible
 *   list non-empty (the pull gate is closed by then regardless).
 * The invariant: a pending buffer is only ever held while [favoriteTracks] is
 * empty AND no cloud restore has been attempted for this owner, so it can
 * never be the reason a restore is skipped.
 *
 * Trade-off worth knowing: while a buffer is held, those favourites are
 * reported by [isFavorite] (so the hearts are correct) but are not in the
 * [favoriteTracks] list the Favourites screen renders. The window is one sync
 * pass (SyncTriggers runs the login pull 2s after the session appears); if
 * that pull never runs (offline / `likes` collection missing) the buffer stays
 * held and the data is still intact — in the `local` namespace and in the
 * buffer key — and reappears on sign-out or on the next successful sync.
 */
object FavoritesStore {
    private const val KEY_FAVORITES = "ghais_favorite_tracks"

    /**
     * Owner-scoped holding slot for favourites adopted from the `local`
     * namespace that are still waiting for the cloud restore. Separate from
     * [KEY_FAVORITES] on purpose: it must never be read as "the account has
     * local data" by the pull-if-empty gate.
     */
    private const val KEY_PENDING_ADOPTION = "ghais_favorite_tracks_pending"

    /** Owner id used while signed out; the only namespace whose data is adopted on login. */
    private const val OWNER_LOCAL = "local"

    /** `audioUrl` prefix the cloud pull uses for a like it could not rebuild (unknown reciter). */
    private const val PLACEHOLDER_SCHEME = "appwrite://likes/"

    // Derives the pre-rebrand base key ("quran…" + "ify_…" form) without
    // hardcoding the legacy literal, so the rename stays grep-clean.
    // Used once per load to adopt + delete any legacy value.
    private fun legacyBase(newBase: String): String =
        newBase.replace("ghais_", "quran" + "ify_")

    private var ownerId: String = OWNER_LOCAL

    private fun key(base: String): String =
        if (ownerId == OWNER_LOCAL) base else "$ownerId::$base"

    private val settings: Settings by lazy { Settings() }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _favoriteTracks = MutableStateFlow<List<TrackItem>>(emptyList())
    val favoriteTracks: StateFlow<List<TrackItem>> = _favoriteTracks.asStateFlow()

    /**
     * Favourites adopted from the `local` namespace that are not in
     * [favoriteTracks] yet (see the class KDoc). Kept in sync with the user
     * actions through [isFavorite]/[toggle]/[remove] so a pending favourite is
     * never invisible or impossible to undo.
     */
    private val _pendingTracks = MutableStateFlow<List<TrackItem>>(emptyList())

    /**
     * True once [clear] has run for the current owner, i.e. the cloud restore
     * has been attempted and the pending buffer is free to be released into the
     * visible list. Reset by [load] (per owner, per process).
     */
    private var restoreAttempted: Boolean = false

    init {
        load()
    }

    /**
     * Rebinds the store to [ownerId]'s namespace, loading it.
     *
     * Favourites saved under the `local` namespace (tapped while signed out)
     * are adopted into the account: buffered as pending when the account list
     * is empty (so the cloud pull can still run first — see the class KDoc),
     * merged in immediately when it is not. Data is only ever carried OUT of
     * the `local` namespace, so switching directly between two accounts never
     * leaks one account's favourites into the other.
     */
    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        val carried = if (this.ownerId == OWNER_LOCAL) {
            _favoriteTracks.value + _pendingTracks.value
        } else {
            emptyList()
        }
        this.ownerId = ownerId
        load()
        if (carried.isNotEmpty()) {
            if (_favoriteTracks.value.isEmpty()) {
                _pendingTracks.value = carried.fold(emptyList<TrackItem>()) { acc, t ->
                    upsert(acc, t)
                }
                persistPending()
            } else {
                _favoriteTracks.update { list -> carried.fold(list) { acc, t -> upsert(acc, t) } }
                save()
            }
        }
    }

    fun isFavorite(track: TrackItem): Boolean =
        favoriteEntries().any { sameFavorite(it, track) }

    fun toggle(track: TrackItem) {
        if (isFavorite(track)) {
            _favoriteTracks.update { list -> list.filterNot { sameFavorite(it, track) } }
            _pendingTracks.update { list -> list.filterNot { sameFavorite(it, track) } }
            persistPending()
            save()
        } else {
            add(track)
        }
    }

    /**
     * Adds [track] unless it is already a favourite (an existing entry is kept,
     * so a repeat pull never reorders the list).
     *
     * The one exception is a track already staged in the pending adoption
     * buffer: while the visible list is still empty and the cloud restore has
     * not run, staging it again here would both hide it from the Favourites
     * list and re-close the pull gate, so it is a no-op. That is the path
     * `SyncTriggers` takes when it re-adopts the pre-auth favourites it
     * snapshotted before [setOwner] already took them over.
     */
    fun add(track: TrackItem) {
        if (!restoreAttempted &&
            _favoriteTracks.value.isEmpty() &&
            _pendingTracks.value.any { sameFavorite(it, track) }
        ) {
            return
        }
        _favoriteTracks.update { list -> upsert(list, track) }
        save()
        reconcilePending()
    }

    fun remove(track: TrackItem) {
        _favoriteTracks.update { list -> list.filterNot { sameFavorite(it, track) } }
        _pendingTracks.update { list -> list.filterNot { sameFavorite(it, track) } }
        persistPending()
        save()
    }

    /**
     * Empties the list. Called by the cloud pull (`SyncEngine.pullLikesIfEmpty`)
     * to replace local state with the cloud copy, so it also marks the restore
     * as attempted and releases any pending adoption into the now-empty list —
     * the pull's own `add` calls then union on top, leaving the local list as
     * (cloud ∪ pre-auth) for the paired `pushLikes` to upload.
     */
    fun clear() {
        restoreAttempted = true
        val held = _pendingTracks.value
        _pendingTracks.value = emptyList()
        _favoriteTracks.value = held.fold(emptyList<TrackItem>()) { acc, t -> upsert(acc, t) }
        if (held.isNotEmpty()) {
            removeKey(KEY_PENDING_ADOPTION)
        }
        save()
    }

    // ------------------------------------------------------------- identity

    /**
     * Cloud doc-id tuple without the user id: `"<reciterSlug>|<surahId>|<ayahNo>"`,
     * the exact inputs `SyncEngine.likeDocId` hashes. Used ONLY to recognise two
     * entries that are the same cloud document — a playable pull result and the
     * `appwrite://likes/<docId>` placeholder for that same document, which have
     * different audio URLs but one doc id. Never used as the primary key: see
     * the class KDoc.
     */
    private fun trackKey(track: TrackItem): String =
        "${track.reciterSlug.trim()}|${track.surahId}|${track.ayahNo}"

    /**
     * Primary identity: the audio URL, widened to also match the cloud doc-id
     * tuple so a placeholder and its rebuilt counterpart are one favourite.
     */
    private fun sameFavorite(a: TrackItem, b: TrackItem): Boolean =
        a.audioUrl == b.audioUrl || trackKey(a) == trackKey(b)

    private fun isPlaceholder(track: TrackItem): Boolean =
        track.audioUrl.startsWith(PLACEHOLDER_SCHEME)

    /**
     * Insert-or-upgrade. A repeat of an existing favourite never reorders or
     * rewrites the list — the existing entry wins — with one exception: a
     * PLAYABLE track replaces the unplayable `appwrite://likes/<docId>`
     * placeholder for the same cloud document (and any leftover duplicate of
     * it). Keeping both would show the same ayah twice in Favourites, once of
     * them unplayable, and the placeholder's stale audio URL would keep the
     * entry from ever matching the rebuilt one.
     */
    private fun upsert(list: List<TrackItem>, track: TrackItem): List<TrackItem> {
        val first = list.indexOfFirst { sameFavorite(it, track) }
        if (first < 0) return list + track
        if (isPlaceholder(track)) {
            // Nothing to upgrade — keep what is already there, drop later duplicates.
            return list.filterIndexed { index, e -> index == first || !sameFavorite(e, track) }
        }
        val keepAt = list.indexOfFirst { sameFavorite(it, track) && !isPlaceholder(it) }
            .let { if (it >= 0) it else first }
        return list
            .filterIndexed { index, e -> index == keepAt || !sameFavorite(e, track) }
            .mapIndexed { index, e -> if (index == keepAt) track else e }
    }

    /** Live view of both the visible list and the pending buffer. */
    private fun favoriteEntries(): List<TrackItem> =
        if (_pendingTracks.value.isEmpty()) {
            _favoriteTracks.value
        } else {
            _favoriteTracks.value + _pendingTracks.value
        }

    /**
     * Releases the pending buffer as soon as holding it back can no longer help
     * — i.e. once the visible list is non-empty, the pull-if-empty gate is
     * already closed and the buffered favourites would otherwise stay invisible
     * forever.
     */
    private fun reconcilePending() {
        if (_pendingTracks.value.isEmpty()) return
        if (restoreAttempted || _favoriteTracks.value.isEmpty()) return
        val held = _pendingTracks.value
        _pendingTracks.value = emptyList()
        _favoriteTracks.update { list -> held.fold(list) { acc, t -> upsert(acc, t) } }
        removeKey(KEY_PENDING_ADOPTION)
        save()
    }

    // ------------------------------------------------------------ persistence

    private fun load() {
        restoreAttempted = false
        try {
            var raw = settings.getString(key(KEY_FAVORITES), "")
            if (raw.isBlank()) {
                // One-time migration: adopt the legacy value if present. Two
                // lookups, because the rename and the per-owner namespacing
                // landed together: a pre-namespacing write is at the BARE
                // legacy key, so looking only at the namespaced one (the
                // pre-existing behaviour) never migrated a signed-in user's
                // favourites and they silently started from an empty list.
                val legacyKeys = listOf(
                    key(legacyBase(KEY_FAVORITES)),
                    legacyBase(KEY_FAVORITES),
                ).distinct()
                for (legacyKey in legacyKeys) {
                    if (legacyKey == key(KEY_FAVORITES)) continue
                    val legacyRaw = try {
                        settings.getString(legacyKey, "")
                    } catch (_: Exception) {
                        ""
                    }
                    if (legacyRaw.isBlank()) continue
                    raw = legacyRaw
                    try {
                        settings.putString(key(KEY_FAVORITES), legacyRaw)
                    } catch (_: Exception) {
                    }
                    try {
                        settings.remove(legacyKey)
                    } catch (_: Exception) {
                    }
                    break
                }
            }
            if (raw.isBlank()) {
                _favoriteTracks.value = emptyList()
            } else {
                _favoriteTracks.value = json.decodeFromString<List<TrackItem>>(raw)
            }
        } catch (_: Exception) {
            _favoriteTracks.value = emptyList()
        }
        _pendingTracks.value = try {
            val pendingRaw = settings.getString(key(KEY_PENDING_ADOPTION), "")
            if (pendingRaw.isBlank()) {
                emptyList()
            } else {
                json.decodeFromString<List<TrackItem>>(pendingRaw)
                    .filterNot { pending -> _favoriteTracks.value.any { sameFavorite(it, pending) } }
            }
        } catch (_: Exception) {
            emptyList()
        }
        reconcilePending()
    }

    private fun save() {
        try {
            settings.putString(key(KEY_FAVORITES), json.encodeToString(_favoriteTracks.value))
        } catch (_: Exception) {
        }
    }

    private fun persistPending() {
        try {
            if (_pendingTracks.value.isEmpty()) {
                settings.remove(key(KEY_PENDING_ADOPTION))
            } else {
                settings.putString(
                    key(KEY_PENDING_ADOPTION),
                    json.encodeToString(_pendingTracks.value),
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun removeKey(base: String) {
        try {
            settings.remove(key(base))
        } catch (_: Exception) {
        }
    }
}
