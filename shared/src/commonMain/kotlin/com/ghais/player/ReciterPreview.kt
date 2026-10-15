package com.ghais.player

import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem
import com.ghais.domain.model.UNKNOWN_DURATION_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 30-second reciter voice preview engine (AGENT A3).
 *
 * Sample verse: Ayat al-Kursi (surah 2, ayah 255) by default.
 *
 * URL policy (`previewUrl`):
 * - PREFERRED: per-ayah EveryAyah mp3 (`Reciter.getAyahAudioUrl`) — works for
 *   ANY catalog because `Models.kt` resolves the folder identically for both:
 *   `audioFolder.ifEmpty { resolveEveryAyahFolder(slug) }`. MP3Quran rows with
 *   a blank `audioFolder` therefore still get a valid EveryAyah clip.
 * - FALLBACK NOTE (mp3quran full-surah): `Reciter.getFullSurahUrl` points at
 *   mp3quran/archive.org full-surah files. It is NOT used as the preview
 *   stream (a whole surah cannot serve as a 30s voice sample without a seek,
 *   and surah 2 is ~80 min). It is only the documented fallback when a
 *   per-ayah clip cannot be resolved — currently `previewUrl` returns null in
 *   that case (blank URL / bad [ayahRef]) and lets the caller surface the
 *   full-surah URL manually via `reciter.getFullSurahUrl(2)`.
 * - Returns null ONLY on unparseable [ayahRef] or blank resolution; never
 *   throws.
 *
 * Queue save/restore: see UNCERTAINTIES below — implemented with the public
 * [AudioEngine] API only.
 */
object ReciterPreview {

    const val DEFAULT_AYAH_REF: String = "2:255"

    /** Auto-stop for the preview stream. */
    const val PREVIEW_DURATION_MS: Long = 30_000L

    private val scope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPreviewing = MutableStateFlow(false)
    val isPreviewing: StateFlow<Boolean> = _isPreviewing.asStateFlow()

    private val _previewSlug = MutableStateFlow<String?>(null)
    val previewSlug: StateFlow<String?> = _previewSlug.asStateFlow()

    // --- Saved user session (captured on playPreview when not previewing) ---
    private var savedQueue: List<TrackItem>? = null
    private var savedIndex: Int = -1
    private var savedPositionMs: Long = 0L
    private var savedWasPlaying: Boolean = false
    private var savedWasAyahMode: Boolean = false
    private var savedTrack: TrackItem? = null

    private var previewJob: Job? = null

    /**
     * Cache key: catalog-scoped (`"<wire>:<slug>:<surah>:<ayah>"`) so the 11
     * slugs that exist in BOTH catalogs never collide.
     */
    fun cacheKey(reciter: Reciter, surahId: Int, ayahNo: Int): String =
        "${reciter.catalog.wire}:${reciter.slug.trim().lowercase()}:$surahId:$ayahNo"

    /** Parses `"2:255"` (also accepts `"2_255"`, `"2-255"`, `"2 255"`). Null on garbage. */
    internal fun parseAyahRef(ayahRef: String): Pair<Int, Int>? {
        val parts = ayahRef.trim().split(':', '_', '-', ' ', '/', '.')
            .map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size != 2) return null
        val surah = parts[0].toIntOrNull() ?: return null
        val ayah = parts[1].toIntOrNull() ?: return null
        if (surah !in 1..114 || ayah !in 1..286) return null
        return surah to ayah
    }

    /**
     * Resolves the playable 30s sample URL for ANY reciter catalog.
     * Consults [PreviewCache] first, stores on miss. Null on bad [ayahRef]
     * or blank resolution — never throws.
     */
    fun previewUrl(reciter: Reciter, ayahRef: String = DEFAULT_AYAH_REF): String? {
        val (surahId, ayahNo) = parseAyahRef(ayahRef) ?: return null
        val key = cacheKey(reciter, surahId, ayahNo)
        PreviewCache.get(key)?.let { return it }
        val url = try {
            reciter.getAyahAudioUrl(surahId, ayahNo)
        } catch (_: Exception) {
            return null
        }
        if (url.isBlank()) return null
        PreviewCache.put(key, url)
        return url
    }

    /**
     * Builds the single-ayah engine track for [playPreview]. Null when the
     * URL cannot be resolved. Never throws (repository lookups guarded).
     */
    fun previewTrack(reciter: Reciter, ayahRef: String = DEFAULT_AYAH_REF): TrackItem? {
        val url = previewUrl(reciter, ayahRef) ?: return null
        val (surahId, ayahNo) = parseAyahRef(ayahRef) ?: return null
        val surah = try {
            QuranDataRepository.getSurahById(surahId)
        } catch (_: Exception) {
            null
        }
        return TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn.ifBlank { reciter.slug },
            surahId = surahId,
            surahNameEn = surah?.nameEn ?: "Surah $surahId",
            surahNameAr = surah?.nameAr ?: "",
            ayahNo = ayahNo,
            audioUrl = url,
            textUthmani = "",
            durationMs = UNKNOWN_DURATION_MS
        )
    }

    /**
     * Starts a 30s preview, preserving the user's session for [stopPreview].
     *
     * @return the resolved sample URL, or null when unresolvable (no-op).
     */
    fun playPreview(reciter: Reciter, ayahRef: String = DEFAULT_AYAH_REF): String? {
        val track = previewTrack(reciter, ayahRef) ?: return null
        if (!_isPreviewing.value) saveSession()
        // Personalization signal (P-series): a preview is a taste vote.
        try {
            com.ghais.personal.SignalStore.recordPreview(reciter.slug)
        } catch (_: Exception) {
        }
        try {
            // Single-ayah queue: AudioEngine routes ayah tracks through
            // playAyah internally (surah-row queue + ayah stream).
            AudioEngine.playQueue(listOf(track), 0)
        } catch (_: Exception) {
            return null
        }
        _previewSlug.value = reciter.slug
        _isPreviewing.value = true
        previewJob?.cancel()
        previewJob = scope.launch {
            try {
                delay(PREVIEW_DURATION_MS)
                // Timeout ends the SAMPLE only; the saved session stays
                // buffered until stopPreview(restore = true) explicitly
                // restores it (no surprise autoplay — see uncertainties).
                stopPreviewInternal(restore = false)
            } catch (_: Exception) {
            }
        }
        return track.audioUrl
    }

    /**
     * Ends the preview. When [restore] is true (default) the session captured
     * by [playPreview] is re-queued at its saved position (autoplay only if
     * it was playing). No-op when not previewing, except still clearing flags.
     */
    fun stopPreview(restore: Boolean = true) {
        stopPreviewInternal(restore)
    }

    private fun stopPreviewInternal(restore: Boolean) {
        try {
            previewJob?.cancel()
        } catch (_: Exception) {
        }
        previewJob = null
        val wasPreviewing = _isPreviewing.value
        _isPreviewing.value = false
        _previewSlug.value = null
        if (!wasPreviewing) return
        if (restore) restoreSession() else clearSession()
    }

    private fun saveSession() {
        try {
            savedQueue = AudioEngine.queue.value.toList()
            savedIndex = AudioEngine.currentIndex.value
            savedPositionMs = AudioEngine.currentPositionMs.value.coerceAtLeast(0L)
            savedWasPlaying = AudioEngine.isPlaying.value
            savedWasAyahMode = AudioEngine.isAyahMode.value
            savedTrack = AudioEngine.currentTrack.value
        } catch (_: Exception) {
            savedQueue = null
            savedIndex = -1
            savedPositionMs = 0L
            savedWasPlaying = false
            savedWasAyahMode = false
            savedTrack = null
        }
    }

    private fun restoreSession() {
        val queue = savedQueue
        val track = savedTrack
        clearSession()
        try {
            if (queue.isNullOrEmpty() || track == null) {
                AudioEngine.stop()
                return
            }
            val index = savedIndexAtRestore(queue, savedIndex)
            if (savedWasAyahMode && track.ayahNo > 0) {
                // Ayah-mode sessions live on a surah-row queue + a detached
                // ayah stream: re-enter through playAyah so the verse (not
                // surah 1:1) resumes at the saved offset.
                AudioEngine.playAyah(track.surahId, track.ayahNo, track.reciterSlug, savedPositionMs)
            } else {
                AudioEngine.playQueue(queue, index, savedPositionMs)
                if (!savedWasPlaying) AudioEngine.pause()
            }
            if (savedWasAyahMode && track.ayahNo > 0 && !savedWasPlaying) {
                AudioEngine.pause()
            }
        } catch (_: Exception) {
        }
    }

    private fun savedIndexAtRestore(queue: List<TrackItem>, index: Int): Int =
        index.coerceIn(0, (queue.size - 1).coerceAtLeast(0))

    private fun clearSession() {
        savedQueue = null
        savedIndex = -1
        savedPositionMs = 0L
        savedWasPlaying = false
        savedWasAyahMode = false
        savedTrack = null
    }
}
