package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import kotlin.random.Random

/**
 * Daily Mix core: pure, deterministic track builder (no UI, no playback).
 *
 * UI wiring comes later — this object only turns "who the user plays most" +
 * "what they already played" into a capped list of playable full-surah
 * [TrackItem]s.
 */
object DailyMix {
    /**
     * Preferred seed qari when the caller has no top qaris. Used ONLY when it
     * genuinely resolves (see [resolveSeedReciter]): [QuranDataRepository.getReciterBySlug]
     * never returns null (it falls back to Mishary on a miss), so resolution
     * is tested with the nullable [QuranDataRepository.getBrowseReciterBySlug].
     */
    const val DEFAULT_SEED_SLUG = "mishary-rashid-alafasy"

    /**
     * Builds the mix.
     *
     * @param topQariSlugs reciter slugs, most-played first (see
     *   [UserUsageRepository.topQariSlugs]). Blank/duplicate slugs are ignored;
     *   empty input falls back to [resolveSeedReciter].
     * @param playedSurahIds (reciterSlug, surahId) pairs to deprioritize. The
     *   slug in each pair may be the stored history spelling or the reciter's
     *   canonical slug (matched case-insensitively).
     * @param daySeed seed for the deterministic shuffle — same inputs + same
     *   seed always yield the same list.
     * @param count maximum tracks returned.
     *
     * Behavior:
     * - Round-robin across the resolved qaris, preferring surahs the qari has
     *   available ([QuranDataRepository.getSurahsForReciter], double-checked
     *   with [Reciter.isSurahAvailable]) that are NOT in [playedSurahIds].
     * - When a qari's unplayed pool is exhausted, falls back to their played
     *   surahs. [playedSurahIds] is an unordered set with no timestamps, so
     *   true least-recently-played order is unknowable here — the fallback
     *   reuses played surahs in seeded-shuffle order (deterministic per
     *   [daySeed]).
     * - Never emits unplayable tracks: every track comes from the reciter's
     *   available-surah list, carries that reciter's canonical slug + full-surah
     *   URL, and is deduped by (slug, surahId), so alias inputs (e.g. "mishary"
     *   + "alafasy" resolving to one reciter) cannot repeat a track.
     * - Track shape mirrors `ReciterProfileScreen`'s full-surah items
     *   (`ayahNo = 0`, `textUthmani = ""`, `durationMs = ayahsCount * 15_000L`).
     */
    fun build(
        topQariSlugs: List<String>,
        playedSurahIds: Set<Pair<String, Int>>,
        daySeed: Long,
        count: Int = 20
    ): List<TrackItem> {
        if (count <= 0) return emptyList()
        val qaris = resolveQaris(topQariSlugs)
        if (qaris.isEmpty()) return emptyList()
        val rng = Random(daySeed)
        val queues: List<ArrayDeque<TrackItem>> = qaris.map { (inputSlug, reciter) ->
            val available = QuranDataRepository.getSurahsForReciter(reciter)
                .filter { reciter.isSurahAvailable(it.id) }
            val (unplayed, played) = available.partition { surah -> !isPlayed(inputSlug, reciter, surah.id, playedSurahIds) }
            // Seeded shuffle both pools: deterministic per daySeed, and a
            // different seed (likely) reorders the mix.
            val ordered = unplayed.shuffled(rng) + played.shuffled(rng)
            ArrayDeque(ordered.map { surah -> surah.toTrackItem(reciter) })
        }
        val out = ArrayList<TrackItem>(minOf(count, queues.sumOf { it.size }))
        val seen = HashSet<Pair<String, Int>>()
        var emitted: Boolean
        do {
            emitted = false
            for (queue in queues) {
                if (out.size >= count) break
                var next: TrackItem? = null
                while (queue.isNotEmpty()) {
                    val candidate = queue.removeFirst()
                    if (seen.add(candidate.reciterSlug to candidate.surahId)) {
                        next = candidate
                        break
                    }
                }
                if (next != null) {
                    out.add(next)
                    emitted = true
                }
            }
        } while (out.size < count && emitted)
        return out
    }

    private fun isPlayed(
        inputSlug: String,
        reciter: Reciter,
        surahId: Int,
        playedSurahIds: Set<Pair<String, Int>>
    ): Boolean {
        if (playedSurahIds.isEmpty()) return false
        return playedSurahIds.any { (slug, id) ->
            id == surahId &&
                (slug.equals(inputSlug, ignoreCase = true) || slug.equals(reciter.slug, ignoreCase = true))
        }
    }

    /**
     * Resolves inputs to (storedSpelling, canonicalReciter) pairs, deduped by
     * canonical slug so aliases share one queue. The browse lookup is tried
     * first (canonical, human-deduped row); anything else falls back to
     * [QuranDataRepository.getReciterBySlug], which always returns a real
     * reciter (never null), so every resolved qari yields playable tracks.
     * Empty/blank-only input resolves to the single [resolveSeedReciter].
     */
    private fun resolveQaris(topQariSlugs: List<String>): List<Pair<String, Reciter>> {
        val inputs = topQariSlugs.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (inputs.isEmpty()) {
            val seed = resolveSeedReciter()
            return listOf(seed.slug to seed)
        }
        val seen = HashSet<String>()
        val out = ArrayList<Pair<String, Reciter>>(inputs.size)
        for (input in inputs) {
            val reciter = QuranDataRepository.getBrowseReciterBySlug(input)
                ?: QuranDataRepository.getReciterBySlug(input)
            if (seen.add(reciter.slug)) {
                out.add(input to reciter)
            }
        }
        if (out.isEmpty()) {
            val seed = resolveSeedReciter()
            return listOf(seed.slug to seed)
        }
        return out
    }

    /**
     * Seed qari for empty input: [DEFAULT_SEED_SLUG] ONLY when it genuinely
     * resolves (nullable browse lookup — the non-null `getReciterBySlug`
     * cannot test this, it silently substitutes the fallback on a miss),
     * else the first browse reciter, else the global fallback (never null).
     */
    private fun resolveSeedReciter(): Reciter {
        QuranDataRepository.getBrowseReciterBySlug(DEFAULT_SEED_SLUG)?.let { return it }
        QuranDataRepository.getBrowseReciters().firstOrNull()?.let { return it }
        return QuranDataRepository.getFallbackReciter()
    }

    /** Full-surah playable shape — mirrors `ReciterProfileScreen`'s items. */
    private fun Surah.toTrackItem(reciter: Reciter): TrackItem = TrackItem(
        reciterSlug = reciter.slug,
        reciterName = reciter.nameEn,
        surahId = id,
        surahNameEn = nameEn,
        surahNameAr = nameAr,
        ayahNo = 0,
        audioUrl = reciter.getFullSurahUrl(id),
        textUthmani = "",
        durationMs = ayahsCount * 15_000L
    )
}
