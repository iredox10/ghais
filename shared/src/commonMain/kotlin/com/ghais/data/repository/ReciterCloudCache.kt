package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.ReciterCatalog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ghais.data.seed.QuranData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cloud overlay for the reciter catalog (Appwrite `reciters` is the source
 * of truth for metadata + photos; bundled seeds stay the offline fallback).
 *
 * Merge rule: cloud docs replace bundled entries by [Reciter.catalogKey];
 * bundled entries with no cloud counterpart are kept, so offline and
 * pre-first-sync behavior is identical to before. Audio resolution is
 * untouched — this only overlays metadata (names, images, flags).
 */
object ReciterCloudCache {
    private val _cloudReciters = MutableStateFlow<List<Reciter>>(emptyList())
    val cloudReciters: StateFlow<List<Reciter>> = _cloudReciters.asStateFlow()

    fun setCloudReciters(reciters: List<Reciter>) {
        _cloudReciters.value = reciters
    }

    /**
     * Cloud pull injected once by `SyncEngine.init` on Android (same pattern
     * as [com.ghais.data.repository.BroadcastRepository.cloudFetcher]); null
     * on platforms without the Appwrite client, where [refresh] is a no-op.
     */
    var catalogFetcher: (suspend () -> List<Reciter>)? = null

    /**
     * Refreshes the overlay from the cloud catalog. Returns true when the
     * cache was replaced. An empty result (offline, error, or a pull that
     * parsed nothing) keeps the previous cache so a transient failure never
     * blanks the catalog back to bundled-only.
     */
    suspend fun refresh(): Boolean {
        return try {
            val list = catalogFetcher?.invoke() ?: return false
            if (list.isEmpty()) return false
            setCloudReciters(list)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Cloud docs keyed by catalog key (`"mp3quran:<slug>"`). */
    private fun cloudByKey(): Map<String, Reciter> =
        _cloudReciters.value.associateBy { it.catalogKey() }

    /**
     * Merged catalog: bundled list with cloud fields overlaid per
     * catalog key, plus cloud-only docs appended. Never throws.
     */
    fun mergedWith(bundled: List<Reciter>): List<Reciter> {
        return try {
            val cloud = cloudByKey()
            if (cloud.isEmpty()) return bundled
            val merged = bundled.map { local -> cloud[local.catalogKey()] ?: local }
            val bundledKeys = bundled.mapTo(HashSet()) { it.catalogKey() }
            merged + cloud.values.filter { it.catalogKey() !in bundledKeys }
        } catch (_: Exception) {
            bundled
        }
    }

    /** Cloud image URL for a slug (either catalog), or null. */
    fun cloudImageForSlug(slug: String): String? {
        if (slug.isBlank()) return null
        val clean = slug.trim().lowercase()
        return try {
            _cloudReciters.value.firstOrNull {
                it.slug.equals(clean, ignoreCase = true) && !it.imageUrl.isNullOrBlank()
            }?.imageUrl?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses the catalog wire value from Appwrite (`"mp3quran"` /
     * `"everyayah"`), defaulting to [ReciterCatalog.MP3QURAN] for legacy
     * docs that predate the field.
     */
    fun catalogOf(raw: Any?): ReciterCatalog {
        return when ((raw as? String)?.trim()?.lowercase()) {
            ReciterCatalog.EVERYAYAH.wire -> ReciterCatalog.EVERYAYAH
            else -> ReciterCatalog.MP3QURAN
        }
    }

    /**
     * Cloud catalog keyed by catalog key, exposed for callers that need to
     * know whether a row came from Appwrite (e.g. a follow resolving to a
     * reciter that only exists in the admin panel).
     */
    fun cloudBySlug(slug: String): Reciter? {
        if (slug.isBlank()) return null
        val clean = slug.trim()
        return _cloudReciters.value.firstOrNull { it.slug.equals(clean, ignoreCase = true) }
    }
}

/**
 * Cloud-merged catalog as Compose state.
 *
 * The cloud pull lands asynchronously (first sync, offline→online, or an
 * admin publish while the app is open), so a screen that captured the list in
 * a keyless `remember {}` would keep showing the pre-sync catalog. Collecting
 * [ReciterCloudCache.cloudReciters] and keying the recomputation on it means
 * reciters uploaded from the admin panel appear as soon as they sync, without
 * the user restarting the app.
 */
@Composable
fun rememberMergedReciters(): List<Reciter> {
    val cloud by ReciterCloudCache.cloudReciters.collectAsState()
    return remember(cloud) { ReciterCloudCache.mergedWith(QuranData.RECITERS) }
}

/** [rememberMergedReciters] with one row per human — see `getBrowseReciters`. */
@Composable
fun rememberBrowseReciters(): List<Reciter> {
    val cloud by ReciterCloudCache.cloudReciters.collectAsState()
    return remember(cloud) { QuranDataRepository.getBrowseReciters() }
}
