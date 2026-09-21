/**
 * Quran MP3 downloads contract for per-(reciter, surah) offline playback.
 *
 * Key scheme: keys are per-(reciter, surah) identifiers formatted as `"<slug>/<surahId>"`
 * (see [DownloadKeys.key]).
 *
 * File layout: `<files>/quran/<slug>/<surahId>.mp3`, where `<files>` is the
 * platform app-files directory.
 *
 * Thread-safety / idempotency: implementations must be thread-safe. Duplicate
 * [QuranDownloads.download] calls for the same key are no-ops while a download
 * is already in flight. [QuranDownloads.delete] cancels any in-flight download
 * for the key and removes any partial file.
 */
package com.ghais.player

import kotlinx.coroutines.flow.StateFlow

/** Key format helpers for per-(reciter,surah) downloads. */
object DownloadKeys {
    fun key(slug: String, surahId: Int): String = "$slug/$surahId"
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
    suspend fun storageBytes(): Long
    suspend fun clearAll()
}
