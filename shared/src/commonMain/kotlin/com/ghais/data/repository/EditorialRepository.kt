package com.ghais.data.repository

import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.UNKNOWN_DURATION_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Editorial (public-catalog) playlist models.
 *
 * Mirrors the live `playlists` / `playlist_items` collections
 * (`playlists{owner_id,title,description,is_public,cover_url}`,
 * `playlist_items{playlist_id,position,reciter_slug,surah_id,ayah_from,ayah_to}`).
 * Admin publishes with `owner_id = "editorial"`, `is_public = true`.
 *
 * Read-only on the client: no create/edit API. Fresh data arrives via
 * `SyncEngine.refreshEditorial()` (android actual), which writes here with
 * [setPlaylists]; the shelf UI only collects [playlists].
 */
@Serializable
data class EditorialPlaylistItem(
    val reciterSlug: String,
    val surahId: Int,
    val ayahFrom: Int = 0,
    val ayahTo: Int = 0,
    val position: Int = 0,
)

@Serializable
data class EditorialPlaylist(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String? = null,
    val items: List<EditorialPlaylistItem> = emptyList(),
)

/**
 * In-memory cache for the editorial catalog (mirrors [ReciterCloudCache]:
 * last-good snapshot, never cleared on a failed pull so offline keeps
 * showing the previous shelf).
 */
object EditorialRepository {
    /** Owner id the admin panel publishes editorial playlists under. */
    const val EDITORIAL_OWNER = "editorial"

    private val _playlists = MutableStateFlow<List<EditorialPlaylist>>(emptyList())
    val playlists: StateFlow<List<EditorialPlaylist>> = _playlists.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setPlaylists(playlists: List<EditorialPlaylist>) {
        _playlists.value = playlists
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun get(id: String): EditorialPlaylist? =
        _playlists.value.firstOrNull { it.id == id }
}

/**
 * Expands an editorial playlist into playable [TrackItem]s, ordered by
 * `position`. Reuses the two existing item → track wirings, never invents:
 * - Full-surah items (`ayahFrom/ayahTo <= 0`): same shape as
 *   [CustomRoutine.toTrackItems] (reciter via
 *   `QuranDataRepository.getReciterBySlug`, skip when
 *   `!reciter.isSurahAvailable`, `audioUrl = reciter.getFullSurahUrl`).
 * - Ayah-range items: one per-ayah track via `reciter.getAyahAudioUrl`,
 *   same shape as `SurahDetailScreen.buildAyahTrack` (textUthmani left blank
 *   in v1 — resolved lazily by the player UI, same as history/likes tracks).
 *   Inverted ranges (`from > to` after coercion) fall back to the full surah
 *   so a bad row still plays instead of vanishing.
 */
fun EditorialPlaylist.toTrackItems(): List<TrackItem> {
    return items.sortedBy { it.position }.flatMap { item ->
        val surah = QuranDataRepository.getSurahById(item.surahId) ?: return@flatMap emptyList()
        val reciter = QuranDataRepository.getReciterBySlug(item.reciterSlug)
        val from = item.ayahFrom.takeIf { it > 0 }
        val to = item.ayahTo.takeIf { it > 0 }
        if (from == null || to == null || from > to) {
            // Full-surah wiring (mirrors CustomRoutine.toTrackItems).
            if (!reciter.isSurahAvailable(surah.id)) return@flatMap emptyList()
            return@flatMap listOf(
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    durationMs = surah.ayahsCount * 15_000L,
                )
            )
        }
        // Ayah-range wiring (mirrors SurahDetailScreen.buildAyahTrack).
        val lo = from.coerceIn(1, surah.ayahsCount)
        val hi = to.coerceIn(lo, surah.ayahsCount)
        (lo..hi).map { ayahNo ->
            TrackItem(
                reciterSlug = reciter.slug,
                reciterName = reciter.nameEn,
                surahId = surah.id,
                surahNameEn = surah.nameEn,
                surahNameAr = surah.nameAr,
                ayahNo = ayahNo,
                audioUrl = reciter.getAyahAudioUrl(surah.id, ayahNo),
                durationMs = UNKNOWN_DURATION_MS,
            )
        }
    }
}
