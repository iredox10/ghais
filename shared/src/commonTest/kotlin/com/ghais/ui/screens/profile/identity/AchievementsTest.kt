package com.ghais.ui.screens.profile.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * AGENT A11 — guards the pure achievement unlock math
 * ([unlockedAchievements] + [isNightHour]).
 * Inputs are primitives only via [AchievementStats]; no stores involved.
 */
class AchievementsTest {

    @Test
    fun freshStatsUnlockNothing() {
        assertTrue(unlockedAchievements(AchievementStats()).isEmpty())
    }

    @Test
    fun firstListenUnlocksOnlyItself() {
        val unlocked = unlockedAchievements(AchievementStats(hasListened = true))
        assertEquals(setOf(ACH_FIRST_LISTEN), unlocked)
    }

    @Test
    fun sevenDayStreakUnlocksDevotion() {
        assertTrue(
            ACH_SEVEN_DAY_DEVOTION in unlockedAchievements(
                AchievementStats(hasListened = true, daysStreak = 7)
            )
        )
        assertFalse(
            ACH_SEVEN_DAY_DEVOTION in unlockedAchievements(
                AchievementStats(hasListened = true, daysStreak = 6)
            )
        )
    }

    @Test
    fun tenVoicesUnlocks() {
        assertTrue(
            ACH_TEN_VOICES in unlockedAchievements(AchievementStats(uniqueReciters = 10))
        )
        assertFalse(
            ACH_TEN_VOICES in unlockedAchievements(AchievementStats(uniqueReciters = 9))
        )
    }

    @Test
    fun nightWindowIsTwentyTwoToFourExclusiveOfFour() {
        for (hour in listOf(22, 23, 0, 1, 2, 3)) {
            assertTrue(isNightHour(hour), "hour $hour should count as night")
            assertTrue(
                ACH_NIGHT_SESSION in unlockedAchievements(AchievementStats(sessionHour24 = hour)),
                "hour $hour should unlock night-session"
            )
        }
        for (hour in listOf(-1, 4, 5, 12, 21, 24)) {
            assertFalse(isNightHour(hour), "hour $hour should not count as night")
            assertFalse(
                ACH_NIGHT_SESSION in unlockedAchievements(AchievementStats(sessionHour24 = hour)),
                "hour $hour should not unlock night-session"
            )
        }
    }

    @Test
    fun khatmaParamUnlocksFirstKhatma() {
        assertTrue(
            ACH_FIRST_KHATMA in unlockedAchievements(AchievementStats(khatmaCompleted = 1))
        )
        assertFalse(
            ACH_FIRST_KHATMA in unlockedAchievements(AchievementStats(khatmaCompleted = 0))
        )
    }

    @Test
    fun thousandMinutesUnlocks() {
        assertTrue(
            ACH_THOUSAND_MINUTES in unlockedAchievements(AchievementStats(totalMinutes = 1000L))
        )
        assertFalse(
            ACH_THOUSAND_MINUTES in unlockedAchievements(AchievementStats(totalMinutes = 999L))
        )
    }

    @Test
    fun masteredParamUnlocksSurahMastered() {
        assertTrue(
            ACH_SURAH_MASTERED in unlockedAchievements(AchievementStats(masteredSurahs = 1))
        )
        assertFalse(
            ACH_SURAH_MASTERED in unlockedAchievements(AchievementStats(masteredSurahs = 0))
        )
    }

    @Test
    fun defsCoverAllSevenAchievements() {
        val ids = ACHIEVEMENTS.map { it.id }.toSet()
        assertEquals(
            setOf(
                ACH_FIRST_LISTEN,
                ACH_SEVEN_DAY_DEVOTION,
                ACH_TEN_VOICES,
                ACH_NIGHT_SESSION,
                ACH_FIRST_KHATMA,
                ACH_THOUSAND_MINUTES,
                ACH_SURAH_MASTERED
            ),
            ids
        )
    }
}
