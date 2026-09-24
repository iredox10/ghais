/**
 * Quran MP3 downloads contract for per-(reciter, surah) offline playback
 * plus per-(reciter, surah, ayah) hifz audio.
 *
 * Key scheme: per-(reciter, surah) keys are `"<slug>/<surahId>"`
 * (see [DownloadKeys.key]); per-ayah keys are `"<slug>/<surahId>/<ayahNo>"`
 * (see [DownloadKeys.ayahKey]).
 *
 * File layout: `<files>/quran/<slug>/<surahId>.mp3` for full surahs,
 * `<files>/quran/<slug>/<surahId>-<ayahNo>.mp3` for single ayahs, where
 * `<files>` is the platform app-files directory.
 *
 * Thread-safety / idempotency: implementations must be thread-safe. Duplicate
 * [QuranDownloads.download]/[QuranDownloads.downloadAyah] calls for the same
 * key are no-ops while a download is already in flight. [QuranDownloads.delete]/
 * [QuranDownloads.deleteAyah] cancels any in-flight download for the key and
 * removes any partial file.
 */
package com.ghais.player

import kotlinx.coroutines.flow.StateFlow

/** Key format helpers for per-(reciter,surah) and per-(reciter,surah,ayah) downloads. */
object DownloadKeys {
    fun key(slug: String, surahId: Int): String = "$slug/$surahId"
    fun ayahKey(slug: String, surahId: Int, ayahNo: Int): String = "$slug/$surahId/$ayahNo"

    /**
     * Every key in a full offline bundle: the surah file plus one per ayah.
     * Hifz rows treat the bundle (not the lone surah key) as the unit of
     * "downloaded" so offline ayah-mode has both audio and (via the
     * prefetched text cache) readable verses.
     */
    fun bundleKeys(slug: String, surahId: Int, ayahCount: Int): List<String> =
        buildList {
            add(key(slug, surahId))
            for (ayahNo in 1..ayahCount) add(ayahKey(slug, surahId, ayahNo))
        }
}

expect object QuranDownloads {
    /**
     * Rebinds the downloaded-KEYS index to the given owner (`"local"` while
     * signed out, else the user id). The index key is namespaced
     * (`"<uid>::<base>"`, bare base for `"local"`) and reloaded, emitting on
     * [downloadedKeys]. Files on disk stay shared (device-level storage);
     * [progress]/[failedKeys] are transient and untouched.
     */
    fun setOwner(ownerId: String)

    /** Keys fully downloaded and playable offline. */
    val downloadedKeys: StateFlow<Set<String>>
    /** In-progress downloads: key -> 0f..1f (indeterminate = -1f). */
    val progress: StateFlow<Map<String, Float>>
    /** Keys whose last download attempt failed. */
    val failedKeys: StateFlow<Set<String>>
    fun isDownloaded(slug: String, surahId: Int): Boolean
    fun progressOf(slug: String, surahId: Int): Float?
    fun download(slug: String, surahId: Int, url: String)
    fun delete(slug: String, surahId: Int)
    /** Local playback URI (file://...) or null when not downloaded. */
    fun localUri(slug: String, surahId: Int): String?
    fun isAyahDownloaded(slug: String, surahId: Int, ayahNo: Int): Boolean
    fun downloadAyah(slug: String, surahId: Int, ayahNo: Int, url: String)
    fun deleteAyah(slug: String, surahId: Int, ayahNo: Int)
    /** Local playback URI (file://...) for a single ayah, or null when not downloaded. */
    fun localAyahUri(slug: String, surahId: Int, ayahNo: Int): String?
    /**
     * Full offline bundle for hifz: the surah file plus every per-ayah clip.
     * Each key stays independently idempotent (already-downloaded / in-flight
     * keys are skipped), so re-tapping a partial bundle only fetches what's
     * missing. Pair with `QuranAyahRepository.prefetchSurah` so verse text is
     * readable offline too.
     */
    fun downloadSurahBundle(slug: String, surahId: Int, surahUrl: String, ayahUrls: Map<Int, String>)
    /** Removes the surah file plus all per-ayah clips for `1..ayahCount`. */
    fun deleteSurahBundle(slug: String, surahId: Int, ayahCount: Int)
    suspend fun storageBytes(): Long
    suspend fun clearAll()
}
