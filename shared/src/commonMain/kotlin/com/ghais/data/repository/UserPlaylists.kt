package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * UserPlaylists: persisted store of user-created playlists.
 *
 * Mirrors [FollowStore]'s persistence approach (per-owner multiplatform
 * Settings + JSON-encoded list, save-on-mutation) but stores a list of
 * [UserPlaylist].
 *
 * ## TrackItem vs TrackRef decision
 * [TrackItem] is already `@Serializable`, so tracks are stored directly —
 * no slim `TrackRef` + resolve-via-`QuranDataRepository` indirection. A
 * stored track therefore keeps playing even if the catalog entry behind it
 * changes, at the cost of the stored display fields (names, URL) going stale
 * if the catalog is rebuilt. Decode is lenient (`ignoreUnknownKeys`), so
 * added [TrackItem] fields never break previously persisted playlists.
 *
 * ## Contract notes (the UI codes against these — do not change shapes)
 * - Blank playlist names are rejected as a no-op: [create] returns `""` and
 *   stores nothing, [rename] leaves the playlist untouched.
 * - Unknown playlist ids are a no-op on every mutator.
 * - [moveTrack] indices are clamped into range; a clamped no-change move
 *   does not rewrite storage.
 * - [removeTrack] drops the FIRST entry whose key matches, so intentional
 *   duplicates (e.g. the same ayah queued twice) survive a single removal.
 * - Nothing here ever throws: persistence failures degrade to an in-memory
 *   change, and corrupt stored JSON loads as an empty list.
 */
object UserPlaylists {
    private const val KEY_PLAYLISTS = "user_playlists_v1"

    private const val OWNER_LOCAL = "local"

    private var ownerId: String = OWNER_LOCAL

    private fun key(base: String): String =
        if (ownerId == OWNER_LOCAL) base else "$ownerId::$base"

    // Same construction fallback as ChartsRepository/QuranAyahRepository: the
    // no-arg `Settings()` factory throws on the JVM unit-test classpath (no
    // platform context), where a process-local map keeps every read/write
    // path executing and testable instead of silently no-oping.
    private val settings: Settings by lazy {
        try {
            Settings()
        } catch (_: Exception) {
            InMemorySettings()
        }
    }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    @Serializable
    data class UserPlaylist(
        val id: String,
        val name: String,
        val tracks: List<TrackItem> = emptyList(),
        val createdAt: Long = 0L,
    )

    init {
        load()
    }

    /**
     * Rebinds the store to [ownerId]'s namespace and reloads their playlists.
     * Same shape as [FollowStore.setOwner].
     */
    fun setOwner(ownerId: String) {
        if (ownerId == this.ownerId) return
        this.ownerId = ownerId
        load()
    }

    /** Creates a playlist, returning its id. A blank name is rejected: returns `""`, stores nothing. */
    fun create(name: String): String {
        try {
            val clean = name.trim()
            if (clean.isBlank()) return ""
            val now = Clock.System.now().toEpochMilliseconds()
            val playlist = UserPlaylist(
                id = "pl-$now-${(0..999999).random()}",
                name = clean,
                tracks = emptyList(),
                createdAt = now,
            )
            _playlists.update { it + playlist }
            save()
            return playlist.id
        } catch (_: Exception) {
            return ""
        }
    }

    /** Renames a playlist. Blank names and unknown ids are a no-op. */
    fun rename(id: String, name: String) {
        try {
            val clean = name.trim()
            if (clean.isBlank()) return
            var changed = false
            _playlists.update { list ->
                list.map { playlist ->
                    if (playlist.id == id) {
                        changed = true
                        playlist.copy(name = clean)
                    } else {
                        playlist
                    }
                }
            }
            if (changed) save()
        } catch (_: Exception) {
        }
    }

    /** Deletes a playlist. Unknown ids are a no-op. */
    fun delete(id: String) {
        try {
            val before = _playlists.value.size
            _playlists.update { list -> list.filterNot { it.id == id } }
            if (_playlists.value.size != before) save()
        } catch (_: Exception) {
        }
    }

    /** Appends [track] to the playlist. Unknown ids are a no-op. Duplicates are kept (order is explicit). */
    fun addTrack(id: String, track: TrackItem) {
        try {
            var changed = false
            _playlists.update { list ->
                list.map { playlist ->
                    if (playlist.id == id) {
                        changed = true
                        playlist.copy(tracks = playlist.tracks + track)
                    } else {
                        playlist
                    }
                }
            }
            if (changed) save()
        } catch (_: Exception) {
        }
    }

    /**
     * Removes the FIRST track whose [trackKeyOf] equals [trackKey].
     * Unknown ids and unmatched keys are a no-op.
     */
    fun removeTrack(id: String, trackKey: String) {
        try {
            var changed = false
            _playlists.update { list ->
                list.map { playlist ->
                    if (playlist.id != id) {
                        playlist
                    } else {
                        val index = playlist.tracks.indexOfFirst { trackKeyOf(it) == trackKey }
                        if (index < 0) {
                            playlist
                        } else {
                            changed = true
                            playlist.copy(tracks = playlist.tracks.filterIndexed { i, _ -> i != index })
                        }
                    }
                }
            }
            if (changed) save()
        } catch (_: Exception) {
        }
    }

    /**
     * Moves the track at [from] to [to]. Both indices are clamped into range;
     * unknown ids and clamped no-change moves are a no-op.
     */
    fun moveTrack(id: String, from: Int, to: Int) {
        try {
            var changed = false
            _playlists.update { list ->
                list.map { playlist ->
                    if (playlist.id != id || playlist.tracks.isEmpty()) {
                        playlist
                    } else {
                        val last = playlist.tracks.lastIndex
                        val src = from.coerceIn(0, last)
                        val dst = to.coerceIn(0, last)
                        if (src == dst) {
                            playlist
                        } else {
                            changed = true
                            val mutable = playlist.tracks.toMutableList()
                            val track = mutable.removeAt(src)
                            mutable.add(dst, track)
                            playlist.copy(tracks = mutable)
                        }
                    }
                }
            }
            if (changed) save()
        } catch (_: Exception) {
        }
    }

    fun get(id: String): UserPlaylist? {
        return try {
            _playlists.value.firstOrNull { it.id == id }
        } catch (_: Exception) {
            null
        }
    }

    /** Stable per-track identity: `"<reciterSlug>:<surahId>:<ayahNo>"`. */
    fun trackKeyOf(track: TrackItem): String =
        "${track.reciterSlug}:${track.surahId}:${track.ayahNo}"

    // ------------------------------------------------------------ persistence

    private fun load() {
        try {
            val raw = settings.getString(key(KEY_PLAYLISTS), "")
            if (raw.isBlank()) {
                _playlists.value = emptyList()
                return
            }
            _playlists.value = json.decodeFromString<List<UserPlaylist>>(raw)
        } catch (_: Exception) {
            _playlists.value = emptyList()
        }
    }

    private fun save() {
        try {
            settings.putString(key(KEY_PLAYLISTS), json.encodeToString(_playlists.value))
        } catch (_: Exception) {
        }
    }

    // Process-local Settings fallback for the JVM unit-test classpath; same
    // shape as the private fallback in ChartsRepository/QuranAyahRepository.
    private class InMemorySettings : Settings {
        private val values = mutableMapOf<String, Any?>()

        override val keys: Set<String> get() = values.keys
        override val size: Int get() = values.size

        override fun clear() {
            values.clear()
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun hasKey(key: String): Boolean = values.containsKey(key)

        override fun putInt(key: String, value: Int) {
            values[key] = value
        }

        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = values[key] as? Int

        override fun putLong(key: String, value: Long) {
            values[key] = value
        }

        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = values[key] as? Long

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun getString(key: String, defaultValue: String): String =
            values[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = values[key] as? String

        override fun putFloat(key: String, value: Float) {
            values[key] = value
        }

        override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = values[key] as? Float

        override fun putDouble(key: String, value: Double) {
            values[key] = value
        }

        override fun getDouble(key: String, defaultValue: Double): Double =
            values[key] as? Double ?: defaultValue

        override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
    }
}
