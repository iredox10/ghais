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

    /**
     * Builds the queue for an offline-source schedule (or the downloads
     * player): every downloaded full surah in [downloadedKeys]
     * (`"<slug>/<surahId>"` entries; single-ayah `"<slug>/<surahId>/<ayahNo>"`
     * clips are skipped), sorted by reciter then surah so Up Next reads as
     * the user's offline shelf.
     *
     * When [onlyKeys] is non-empty, only those keys play (a schedule's saved
     * selection); files deleted since are dropped silently. Empty [onlyKeys]
     * means "everything downloaded".
     *
     * A key whose reciter is unknown to [reciters] still gets a track (with a
     * synthetic identity): the player resolves the downloaded file first, so
     * the stream URL only matters for non-downloaded siblings — and here
     * every track IS a download. Returns empty when nothing qualifies.
     */
    fun buildOfflineTracks(
        downloadedKeys: Collection<String>,
        reciters: List<Reciter>,
        surahs: List<Surah>,
        onlyKeys: Set<String> = emptySet(),
    ): List<TrackItem> {
        data class Key(val slug: String, val surahId: Int)
        val selected = onlyKeys.takeIf { it.isNotEmpty() }
        val parsed = downloadedKeys.mapNotNull { key ->
            if (selected != null && key !in selected) return@mapNotNull null
            val parts = key.split("/")
            if (parts.size != 2) return@mapNotNull null
            val surahId = parts[1].toIntOrNull() ?: return@mapNotNull null
            if (parts[0].isBlank() || surahId !in 1..114) return@mapNotNull null
            Key(parts[0], surahId)
        }.sortedWith(compareBy({ it.slug }, { it.surahId }))
        return parsed.mapNotNull { (slug, surahId) ->
            val reciter = reciters.firstOrNull { it.slug == slug }
                ?: Reciter(slug = slug, nameEn = slug, nameAr = "")
            val surah = surahs.firstOrNull { it.id == surahId }
            TrackItem(
                reciterSlug = reciter.slug,
                reciterName = reciter.nameEn.ifBlank { slug },
                surahId = surahId,
                surahNameEn = surah?.nameEn?.ifBlank { null } ?: "Surah $surahId",
                surahNameAr = surah?.nameAr.orEmpty(),
                ayahNo = 0,
                audioUrl = runCatching { reciter.getFullSurahUrl(surahId) }.getOrNull().orEmpty(),
                durationMs = (surah?.ayahsCount ?: 0) * 15_000L,
            )
        }
    }
}
