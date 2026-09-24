package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.ReciterCatalog
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
}
