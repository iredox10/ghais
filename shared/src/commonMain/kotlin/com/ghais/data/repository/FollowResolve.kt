package com.ghais.data.repository

import com.ghais.data.seed.ALL_MP3QURAN_RECITERS
import com.ghais.data.seed.QuranData
import com.ghais.data.seed.GhaisAssets
import com.ghais.data.seed.QuranDataRepository as SeedRepository
import com.ghais.domain.model.Reciter

/**
 * A followed qari resolved to something displayable, no matter which slug
 * namespace stored it. Follow buttons across the app store different slug
 * flavors (legacy `Reciter` slugs like "alafasy", detailed slugs like
 * "mishary", MP3Quran slugs) — this resolves all of them to one canonical
 * legacy [Reciter] (safe for navigation + photoForSlug) plus the best photo.
 */
data class ResolvedQari(
    /** Raw slug as stored in [FollowStore] — use for toggle/unfollow. */
    val storedSlug: String,
    /** Canonical reciter for display + profile navigation. */
    val reciter: Reciter,
    /** Best photo URL across sources, or null for initial fallback. */
    val photoUrl: String?,
)

/**
 * Resolve a stored follow slug to a displayable qari, or null when the slug
 * matches nothing (caller skips it).
 */
fun resolveFollowedQari(rawSlug: String): ResolvedQari? {
    val normalized = rawSlug.trim().lowercase()
    if (normalized.isEmpty()) return null

    val verified = GhaisAssets.VerifiedReciters.find {
        it.slug.equals(rawSlug, ignoreCase = true)
    }
    val detailed = SeedRepository.getReciterBySlug(rawSlug)
    val legacy = QuranData.RECITERS.find {
        it.slug.lowercase() == normalized ||
            it.slug.replace("_", "-").lowercase() == normalized.replace("_", "-")
    }
    val mp3 = ALL_MP3QURAN_RECITERS.find { it.slug.lowercase() == normalized }

    if (verified == null && detailed == null && legacy == null && mp3 == null) return null

    // Canonical reciter: legacy model wins (navigation + photoForSlug speak it),
    // then MP3Quran entry (already a Reciter), then synthesize from detailed.
    val reciter: Reciter = legacy
        ?: mp3
        ?: detailed?.let {
            Reciter(
                slug = it.slug,
                nameEn = it.nameEn,
                nameAr = it.nameAr,
                riwayah = it.riwayah,
                style = it.style,
                tempo = it.tempo,
                country = it.country,
            )
        }
        ?: return null

    // If we only matched via verified/detailed but have a legacy twin, prefer it.
    val canonical = if (legacy == null) {
        val twin = QuranData.RECITERS.find {
            it.nameEn.equals(reciter.nameEn, ignoreCase = true)
        }
        twin ?: reciter
    } else {
        reciter
    }

    val photo = verified?.photoUrl ?: detailed?.photoUrl
    return ResolvedQari(
        storedSlug = rawSlug,
        reciter = canonical,
        photoUrl = photo,
    )
}
