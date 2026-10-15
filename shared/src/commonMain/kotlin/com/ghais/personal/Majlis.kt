package com.ghais.personal

import com.ghais.data.repository.QuranDataRepository
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem

/** Fallback reciter when the user follows nobody yet (global top voice). */
const val DEFAULT_MAJLIS_RECITER_SLUG = "mishary"

/** Surah id for Al-Fatiha — always first (endowed progress). */
const val MAJLIS_OPENING_SURAH_ID = 1

/**
 * Activation-funnel Majlis builder: quiz payoff queue.
 *
 * Pure function, no I/O: [goalId] + [reciter] + [minutesCap] → capped
 * [TrackItem] list. Al-Fatiha always opens (endowed progress — the first
 * item feels already-done), then goal-ordered surahs, each estimated at
 * `ayahsCount * 15s` like the reciter reels. At least one track is
 * returned whenever the reciter has any available surah.
 *
 * Goal → surah order (short, high-completion first; the long goal surah
 * anchors the end so the queue reads as a session, not a wall):
 * - sleep: Mulk (67), Ya-Sin (36), Ikhlas..Nas, then Kahf (18)
 * - memorize: Ikhlas (112), Falaq (113), Nas (114), Fatiha, Mulk
 * - work/study: Kahf (18), Ya-Sin (36), Rahman (55), short closers
 * - default/consistency: Ya-Sin (36), Rahman (55), Mulk (67), Kahf (18)
 */
fun majlisSurahIds(goalId: String?): List<Int> = when (goalId?.trim()?.lowercase()) {
    "sleep" -> listOf(1, 67, 36, 112, 113, 114, 55, 18)
    "memorize" -> listOf(1, 112, 113, 114, 108, 67, 36)
    "work", "study" -> listOf(1, 18, 36, 55, 112, 113, 114)
    else -> listOf(1, 36, 55, 67, 18, 112, 113, 114)
}

/**
 * Builds the Majlis queue for [reciter], capped at [minutesCap] of
 * estimated audio. Unavailable surahs are skipped; unknown reciter falls
 * back to catalog lookup by [DEFAULT_MAJLIS_RECITER_SLUG]. Never throws —
 * failures return empty.
 */
fun buildMajlisTracks(
    reciter: Reciter,
    goalId: String?,
    minutesCap: Int = 10,
): List<TrackItem> = try {
    val capMs = minutesCap.coerceIn(5, 180) * 60_000L
    val ids = majlisSurahIds(goalId)
        .filter { reciter.isSurahAvailable(it) }
        .ifEmpty {
            QuranDataRepository.getSurahsForReciter(reciter)
                .filter { reciter.isSurahAvailable(it.id) }
                .take(3).map { it.id }
        }
    val tracks = mutableListOf<TrackItem>()
    var totalMs = 0L
    for (id in ids) {
        val surah = QuranDataRepository.getSurahById(id) ?: continue
        val durationMs = surah.ayahsCount * 15_000L
        if (tracks.isNotEmpty() && totalMs + durationMs > capMs) break
        tracks += TrackItem(
            reciterSlug = reciter.slug,
            reciterName = reciter.nameEn,
            surahId = surah.id,
            surahNameEn = surah.nameEn,
            surahNameAr = surah.nameAr,
            ayahNo = 0,
            audioUrl = reciter.getFullSurahUrl(surah.id),
            textUthmani = "",
            durationMs = durationMs,
        )
        totalMs += durationMs
    }
    tracks
} catch (_: Exception) {
    emptyList()
}

/** Resolves the Majlis voice: first followed reciter, else global default. */
fun resolveMajlisReciter(followedSlugs: Set<String>): Reciter = try {
    val first = followedSlugs.firstOrNull()?.takeIf { it.isNotBlank() }
    if (first != null) QuranDataRepository.getReciterBySlug(first)
    else QuranDataRepository.getReciterBySlug(DEFAULT_MAJLIS_RECITER_SLUG)
} catch (_: Exception) {
    QuranDataRepository.getReciterBySlug(DEFAULT_MAJLIS_RECITER_SLUG)
}
