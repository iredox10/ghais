package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import com.ghais.domain.model.TrackItem
import kotlin.math.max
import kotlin.math.min

/**
 * Pure builder for the full-surah [TrackItem] queue of a scheduled recitation.
 *
 * Expands a [RecitationSchedule]'s `fromSurah..toSurah` range (normalized, so
 * a swapped pair still plays), keeps only surahs the reciter has actually
 * recorded ([Reciter.isSurahAvailable]), and stamps each track with the
 * reciter's identity, the surah names, a zero ayah number (full-surah audio)
 * and an estimated `ayahsCount * 15s` duration.
 *
 * Deliberately free of Android, context and repository singletons: callers
 * resolve the [RecitationSchedule] (e.g. via [SchedulesStore.get]), the
 * [Reciter] and the surah list themselves and pass them in, which keeps this
 * shareable between the alarm receiver, playback and tests.
 */
object ScheduleTracks {

    /**
     * Builds the ordered track queue for [schedule]'s range over [surahs].
     * Returns an empty list when the (normalized) range is empty or no surah
     * in it is available for [reciter] — callers should not queue that.
     */
    fun buildScheduleTracks(
        schedule: RecitationSchedule,
        reciter: Reciter,
        surahs: List<Surah>,
    ): List<TrackItem> {
        val from = min(schedule.fromSurah, schedule.toSurah)
        val to = max(schedule.fromSurah, schedule.toSurah)
        return surahs
            .filter { it.id in from..to && reciter.isSurahAvailable(it.id) }
            .map { s ->
                TrackItem(
                    reciterSlug = reciter.slug,
                    reciterName = reciter.nameEn,
                    surahId = s.id,
                    surahNameEn = s.nameEn,
                    surahNameAr = s.nameAr,
                    ayahNo = 0,
                    audioUrl = reciter.getFullSurahUrl(s.id),
                    durationMs = s.ayahsCount * 15_000L,
                )
            }
    }
}
