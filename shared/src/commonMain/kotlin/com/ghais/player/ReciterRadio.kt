package com.ghais.player

import com.ghais.data.repository.QuranDataRepository
import com.ghais.data.repository.RelatedQaris
import com.ghais.domain.model.Reciter
import com.ghais.domain.model.TrackItem

/**
 * Reciter radio: builds a "station" queue seeded by one qari, then interleaved
 * with similar qaris for discovery.
 *
 * Similarity ranking is delegated to [RelatedQaris.relatedTo] (country +
 * riwayah + style scoring with alias-twin exclusion), capped to 8 voices.
 *
 * Track shape is identical to the reciter profile's `playableTracks`
 * (ReciterProfileScreen: full-surah items, `ayahNo = 0`, `textUthmani = ""`,
 * `durationMs = ayahsCount * 15_000L`), and only surahs passing
 * [Reciter.isSurahAvailable] are ever emitted, so the queue never holds a
 * known-404 URL. Emission is deterministic (no shuffle) and never puts the
 * same surah twice in a row from the same qari.
 */
object ReciterRadio {

    /** Voices rotated after the seed in the discovery half of the station. */
    private const val MAX_SIMILAR_QARIS: Int = 8

    /**
     * Builds a station queue: seed-heavy for the first half (seed on every
     * even position, similar qaris on odds), then an even rotation over the
     * seed + similar voices for discovery.
     */
    fun stationQueue(seedSlug: String, count: Int = 25): List<TrackItem> {
        if (count <= 0) return emptyList()
        val seed = QuranDataRepository.getReciterBySlug(seedSlug)
        val similar = rankSimilarToSeed(seed)
        val lineup = listOf(seed) + similar

        val tracksBySlug: Map<String, List<TrackItem>> = lineup.associate { reciter ->
            reciter.slug to buildPlayableTracks(reciter)
        }
        // Defensive: a reciter row with zero playable surahs contributes no
        // turns (a blank availableSurahList means all 114, so the seed is
        // effectively never empty — this only guards cloud-only oddities).
        // The first active voice keeps the seed-heavy slot when the seed
        // itself has nothing playable.
        val activeSimilar = similar.filter { tracksBySlug.getValue(it.slug).isNotEmpty() }
        val activeLineup = buildList {
            if (tracksBySlug.getValue(seed.slug).isNotEmpty()) add(seed)
            addAll(activeSimilar)
        }
        if (activeLineup.isEmpty()) return emptyList()
        val heavy: Reciter = activeLineup.first()

        val cursors = mutableMapOf<String, Int>()
        val out = ArrayList<TrackItem>(count)
        var position = 0
        // Bounded: single-surah voices skip turns that would duplicate the
        // previous item, so cap total iterations instead of looping forever.
        var guard = 0
        val maxIterations = count * (activeLineup.size + 1) + activeLineup.size
        while (out.size < count && guard < maxIterations) {
            guard++
            val reciter = pickReciter(position++, heavy, activeSimilar, activeLineup, count)
            val tracks = tracksBySlug.getValue(reciter.slug)
            if (tracks.isEmpty()) continue
            val previous = out.lastOrNull()
            var index = cursors.getOrDefault(reciter.slug, 0)
            var candidate: TrackItem? = null
            var attempts = 0
            while (attempts < tracks.size) {
                val track = tracks[index % tracks.size]
                index = (index + 1) % tracks.size
                attempts++
                if (previous != null &&
                    previous.reciterSlug == track.reciterSlug &&
                    previous.surahId == track.surahId
                ) {
                    continue
                }
                candidate = track
                break
            }
            if (candidate == null) continue
            cursors[reciter.slug] = index
            out.add(candidate)
        }
        return out
    }

    private fun pickReciter(
        position: Int,
        heavy: Reciter,
        similar: List<Reciter>,
        lineup: List<Reciter>,
        count: Int
    ): Reciter {
        if (similar.isEmpty() || lineup.size < 2) return lineup.first()
        val half = (count + 1) / 2
        return if (position < half) {
            if (position % 2 == 0) heavy else similar[(position / 2) % similar.size]
        } else {
            lineup[position % lineup.size]
        }
    }

    /**
     * Similar voices via [RelatedQaris] (same country/riwayah/style scoring,
     * seed + alias-twins excluded), capped to [MAX_SIMILAR_QARIS].
     */
    private fun rankSimilarToSeed(seed: Reciter): List<Reciter> {
        val browse = try {
            QuranDataRepository.getBrowseReciters()
        } catch (_: Exception) {
            return emptyList()
        }
        return RelatedQaris.relatedTo(seed, browse, MAX_SIMILAR_QARIS)
    }

    /**
     * Full-surah playable items in the exact shape of the reciter profile's
     * `playableTracks` builder: `ayahNo = 0`, `textUthmani = ""`,
     * `durationMs = ayahsCount * 15_000L`, filtered to available surahs only.
     */
    private fun buildPlayableTracks(reciter: Reciter): List<TrackItem> =
        QuranDataRepository.getSurahsForReciter(reciter)
            .filter { reciter.isSurahAvailable(it.id) }
            .map { surah ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = surah.id,
                    surahNameEn = surah.nameEn,
                    surahNameAr = surah.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(surah.id),
                    textUthmani = "",
                    durationMs = surah.ayahsCount * 15_000L
                )
            }
}
