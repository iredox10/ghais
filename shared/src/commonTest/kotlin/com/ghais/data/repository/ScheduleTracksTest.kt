package com.ghais.data.repository

import com.ghais.domain.model.Reciter
import com.ghais.domain.model.Surah
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the pure scheduled-playback track builder
 * ([ScheduleTracks.buildScheduleTracks]), which was lifted verbatim out of
 * `ScheduleAlarmReceiver` so the alarm path, `SchedulePlayback` and tests all
 * share one implementation.
 *
 * Contracts covered here:
 *
 * 1. Range normalization: `fromSurah > toSurah` (a stored schedule that
 *    skipped `SchedulesStore.normalize`) must still yield the same ascending
 *    queue as the swapped pair, not an empty one.
 * 2. Availability filter: a surah the reciter has not recorded is dropped
 *    (`Reciter.isSurahAvailable`), and an entirely unavailable range yields
 *    an empty list so the caller can bail before queueing.
 * 3. Track shape: `ayahNo` is always 0 (full-surah audio, never per-ayah),
 *    `durationMs` is the `ayahsCount * 15_000L` estimate, and the reciter
 *    slug/name plus `getFullSurahUrl` audio URL propagate untouched.
 *
 * Everything is offline and deterministic: fixtures are plain `Reciter` /
 * `Surah` constructor calls, no repository singletons, no platform player.
 */
class ScheduleTracksTest {

    /** Real `Surah` constructor; only the fields the builder reads vary. */
    private fun surah(id: Int, ayahsCount: Int) = Surah(
        id = id,
        nameEn = "Surah $id",
        nameAr = "سورة $id",
        transliteration = "Surah $id",
        ayahsCount = ayahsCount,
        revelationType = "Meccan",
    )

    /** Small catalog: surahs 1..5 with distinct ayah counts. */
    private val surahs = listOf(
        surah(1, 7),
        surah(2, 286),
        surah(3, 200),
        surah(4, 176),
        surah(5, 120),
    )

    /**
     * Real `Reciter` minimal constructor (slug/nameEn/nameAr are the only
     * required args); `serverUrl` pins `getFullSurahUrl` to a deterministic
     * base and `availableSurahList` drives the availability filter.
     */
    private fun reciter(
        availableSurahList: String = "",
        serverUrl: String = "https://server8.mp3quran.net/afs/",
    ) = Reciter(
        slug = "alafasy",
        nameEn = "Mishary Rashid Alafasy",
        nameAr = "مشاري راشد العفاسي",
        serverUrl = serverUrl,
        availableSurahList = availableSurahList,
    )

    private fun schedule(fromSurah: Int, toSurah: Int) = RecitationSchedule(
        id = "sched-1",
        hour = 5,
        minute = 30,
        reciterSlug = "alafasy",
        fromSurah = fromSurah,
        toSurah = toSurah,
    )

    // --- 1. Range normalization ---

    @Test
    fun testSwappedRangeIsNormalizedToAscendingOrder() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(5, 2), reciter(), surahs)
        assertEquals(listOf(2, 3, 4, 5), tracks.map { it.surahId })
    }

    @Test
    fun testSwappedRangeMatchesDirectRange() {
        val swapped = ScheduleTracks.buildScheduleTracks(schedule(5, 2), reciter(), surahs)
        val direct = ScheduleTracks.buildScheduleTracks(schedule(2, 5), reciter(), surahs)
        assertEquals(direct, swapped)
    }

    @Test
    fun testRangeIsClampedToTheProvidedSurahs() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(1, 114), reciter(), surahs)
        assertEquals(listOf(1, 2, 3, 4, 5), tracks.map { it.surahId })
    }

    // --- 2. Availability filter ---

    @Test
    fun testUnavailableSurahsAreFilteredOut() {
        val tracks = ScheduleTracks.buildScheduleTracks(
            schedule(1, 5),
            reciter(availableSurahList = "1,3,5"),
            surahs,
        )
        assertEquals(listOf(1, 3, 5), tracks.map { it.surahId })
    }

    @Test
    fun testEmptyWhenNoSurahInRangeIsAvailable() {
        val tracks = ScheduleTracks.buildScheduleTracks(
            schedule(1, 5),
            reciter(availableSurahList = "114"),
            surahs,
        )
        assertTrue(tracks.isEmpty())
    }

    @Test
    fun testEmptyWhenRangeFallsOutsideAvailableSurahs() {
        val tracks = ScheduleTracks.buildScheduleTracks(
            schedule(5, 2),
            reciter(availableSurahList = "1"),
            surahs,
        )
        assertTrue(tracks.isEmpty())
    }

    // --- 3. Track shape ---

    @Test
    fun testAyahNoIsAlwaysZero() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(1, 5), reciter(), surahs)
        assertTrue(tracks.isNotEmpty())
        assertTrue(tracks.all { it.ayahNo == 0 })
    }

    @Test
    fun testDurationMsIsAyahCountTimesFifteenSeconds() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(2, 3), reciter(), surahs)
        assertEquals(listOf(2, 3), tracks.map { it.surahId })
        assertEquals(286 * 15_000L, tracks[0].durationMs)
        assertEquals(200 * 15_000L, tracks[1].durationMs)
    }

    @Test
    fun testDurationMsMatchesEverySurahInQueue() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(1, 5), reciter(), surahs)
        tracks.forEach { track ->
            val source = surahs.first { it.id == track.surahId }
            assertEquals(source.ayahsCount * 15_000L, track.durationMs)
        }
    }

    // --- 4. Reciter / surah propagation ---

    @Test
    fun testReciterSlugNameAndAudioUrlPropagate() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(1, 3), reciter(), surahs)
        assertTrue(tracks.isNotEmpty())
        tracks.forEach { track ->
            assertEquals("alafasy", track.reciterSlug)
            assertEquals("Mishary Rashid Alafasy", track.reciterName)
            assertEquals(
                "https://server8.mp3quran.net/afs/" +
                    track.surahId.toString().padStart(3, '0') + ".mp3",
                track.audioUrl,
            )
        }
    }

    @Test
    fun testAudioUrlComesFromReciterFullSurahUrl() {
        val r = reciter(availableSurahList = "1,2")
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(1, 2), r, surahs)
        assertEquals(
            listOf(r.getFullSurahUrl(1), r.getFullSurahUrl(2)),
            tracks.map { it.audioUrl },
        )
    }

    @Test
    fun testSurahNamesPropagate() {
        val tracks = ScheduleTracks.buildScheduleTracks(schedule(2, 2), reciter(), surahs)
        val track = tracks.single()
        assertEquals(2, track.surahId)
        assertEquals("Surah 2", track.surahNameEn)
        assertEquals("سورة 2", track.surahNameAr)
    }
}
